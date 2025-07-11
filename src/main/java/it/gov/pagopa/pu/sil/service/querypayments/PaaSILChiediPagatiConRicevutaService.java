package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.service.debtpositions.DebtPositionFacadeService;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPagatiConRicevuta;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPagatiConRicevutaRisposta;
import jakarta.activation.DataHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@Slf4j
public class PaaSILChiediPagatiConRicevutaService extends AbstractQueryPaymentsService<PaaSILChiediPagatiConRicevuta, PaaSILChiediPagatiConRicevutaRisposta> {

  private final DebtPositionService debtPositionService;
  private final PagatiMapper pagatiMapper;
  private final SessionIdMapper sessionIdMapper;
  private final ReceiptService receiptService;
  private SilFaults debtPositionNotFoundFault;

  public PaaSILChiediPagatiConRicevutaService(
    OrganizationService organizationService,
    DebtPositionService debtPositionService,
    PagatiMapper pagatiMapper,
    ReceiptService receiptService,
    SessionIdMapper sessionIdMapper) {
    super(organizationService);
    this.debtPositionService = debtPositionService;
    this.pagatiMapper = pagatiMapper;
    this.receiptService = receiptService;
    this.sessionIdMapper = sessionIdMapper;
  }

  @Override
  protected void validateRequest(PaaSILChiediPagatiConRicevuta request) {
    //validate that only one search field is present
    if(Stream.of(request.getIdSession(), request.getIdentificativoUnivocoDovuto(), request.getIdentificativoUnivocoVersamento())
      .filter(StringUtils::isNotBlank)
      .count() != 1) {
      throw new SilFaultException(SilFaults.PAA_SYSTEM_ERROR, "Errore, è obbligatorio specificare esattamente un parametro tra idSession, identificativoUnivocoVersamento e identificativoUnivocoDovuto.");
    }
  }

  @Override
  protected SilFaults getFaultForDebtPositionNotFound() {
    return debtPositionNotFoundFault;
  }

  @Override
  protected List<Pair<DebtPositionDTO, InstallmentDTO>> getDebtPositionsAndInstallments(PaaSILChiediPagatiConRicevuta request, Organization organization, String accessToken) {

    if (StringUtils.isNotBlank(request.getIdSession())) {
      debtPositionNotFoundFault = SilFaults.PAA_ID_SESSION_NON_VALIDO;

      return sessionIdMapper.mapSessionIdToInstallmentIds(request.getIdSession()).stream()
        //search for the debt position by installmentId
        .map(installmentId -> Pair.of(installmentId, debtPositionService.getDebtPositionByInstallmentId(installmentId, accessToken)))
        //find the installment in the debt position
        .map(debtPositionPair -> Pair.of(debtPositionPair.getRight(), findInstallmentOfDebtPosition(debtPositionPair.getRight(),
          installment -> Objects.equals(installment.getInstallmentId(), debtPositionPair.getLeft()))))
        //return the pair of debt position and matching installment
        .toList();
    } else if (StringUtils.isNotBlank(request.getIdentificativoUnivocoDovuto())) {
      debtPositionNotFoundFault = SilFaults.PAA_IUD_NON_VALIDO;

      return debtPositionService.getDebtPositionsByOrganizationIdAndIud(
          organization.getOrganizationId(), request.getIdentificativoUnivocoDovuto(), DebtPositionFacadeService.ALLOWED_ORIGINS, accessToken)
        .stream().filter(dp -> !Objects.equals(dp.getStatus(), DebtPositionStatus.CANCELLED))
        .findFirst()
        .map(debtPosition -> Pair.of(debtPosition, findInstallmentOfDebtPosition(debtPosition,
          installment -> request.getIdentificativoUnivocoDovuto().equals(installment.getIud()))))
        .map(List::of)
        .orElse(List.of());
    } else {
      debtPositionNotFoundFault = SilFaults.PAA_IUV_NON_VALIDO;

      //search for the debt position by identificativoUnivocoVersamento
      return debtPositionService.getDebtPositionsByOrganizationIdAndIuv(
          organization.getOrganizationId(), request.getIdentificativoUnivocoVersamento(), DebtPositionFacadeService.ALLOWED_ORIGINS, accessToken)
        .stream().filter(dp -> !Objects.equals(dp.getStatus(), DebtPositionStatus.CANCELLED))
        .findFirst()
        .map(debtPosition -> Pair.of(debtPosition, findInstallmentOfDebtPosition(debtPosition,
          installment -> request.getIdentificativoUnivocoVersamento().equals(installment.getIuv()))))
        .map(List::of)
        .orElse(List.of());
    }
  }


  @Override
  protected String getOrgIpaCode(PaaSILChiediPagatiConRicevuta request) {
    return request.getCodIpaEnte();
  }

  @Override
  protected PaaSILChiediPagatiConRicevutaRisposta mapper(List<Pair<DebtPositionDTO, InstallmentDTO>> debtPositionWithInstallmentList,
                                                         Organization organization, String accessToken) {

    //TODO currently support only one debt position and installment, but could be extended to support multiple
    InstallmentDTO installmentDTO = debtPositionWithInstallmentList.getFirst().getRight();

    //map debt position to Pagati
    byte[] encodedPagati = pagatiMapper.mapDebtPositionsToEncodedPagatiConRicevuta(installmentDTO, organization, accessToken);

    //retrieve the original receipt
    byte[] encodedReceipt = receiptService.getReceiptById(installmentDTO.getReceiptId(), organization.getOrganizationId(), accessToken);

    // Prepare the response
    PaaSILChiediPagatiConRicevutaRisposta response = new PaaSILChiediPagatiConRicevutaRisposta();
    response.setPagati(new DataHandler(new ByteArrayDataSource("application/octet-stream", encodedPagati)));
    response.setRt(new DataHandler(new ByteArrayDataSource("application/octet-stream", encodedReceipt)));
    return response;
  }
}
