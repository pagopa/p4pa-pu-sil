package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.QueryPaymentStatusType;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionInstallmentFacadeService;
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
import java.util.stream.Stream;

@Service
@Slf4j
public class PaaSILChiediPagatiConRicevutaService extends AbstractQueryPaymentsService<PaaSILChiediPagatiConRicevuta, PaaSILChiediPagatiConRicevutaRisposta> {

  private final PagatiMapper pagatiMapper;
  private final ReceiptService receiptService;

  public PaaSILChiediPagatiConRicevutaService(
    OrganizationService organizationService,
    DebtPositionInstallmentFacadeService debtPositionInstallmentFacadeService,
    PagatiMapper pagatiMapper,
    ReceiptService receiptService) {
    super(organizationService, debtPositionInstallmentFacadeService);
    this.pagatiMapper = pagatiMapper;
    this.receiptService = receiptService;
  }

  @Override
  protected String getOrgIpaCode(PaaSILChiediPagatiConRicevuta request) {
    return request.getCodIpaEnte();
  }

  @Override
  protected PaaSILChiediPagatiConRicevutaRisposta mapper(List<Pair<DebtPositionDTO, InstallmentDTO>> debtPositionWithInstallmentList,
                                                         Organization organization, String accessToken, PaaSILChiediPagatiConRicevuta request) {

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

  @Override
  protected PaymentStatusRequest validateAndTransformRequest(PaaSILChiediPagatiConRicevuta request, String orgIpaCode) {
    String id = null;
    QueryPaymentStatusType type = null;

    //validate that only one search field is present
    if(Stream.of(request.getIdSession(), request.getIdentificativoUnivocoDovuto(), request.getIdentificativoUnivocoVersamento())
      .filter(StringUtils::isNotBlank)
      .count() != 1) {
      throw new SilFaultException(SilFaults.PAA_SYSTEM_ERROR, "Errore, è obbligatorio specificare esattamente un parametro tra idSession, identificativoUnivocoVersamento e identificativoUnivocoDovuto.");
    }
    if (StringUtils.isNotBlank(request.getIdSession())) {
      id = request.getIdSession();
      type = QueryPaymentStatusType.INSTALLMENT_ID;
    } else if (StringUtils.isNotBlank(request.getIdentificativoUnivocoVersamento())) {
      id = request.getIdentificativoUnivocoVersamento();
      type = QueryPaymentStatusType.NOTICE_NUMBER;
    } else if (StringUtils.isNotBlank(request.getIdentificativoUnivocoDovuto())) {
      id = request.getIdentificativoUnivocoDovuto();
      type = QueryPaymentStatusType.IUD;
    }
    return new PaymentStatusRequest(orgIpaCode, type, id, false);
  }
}
