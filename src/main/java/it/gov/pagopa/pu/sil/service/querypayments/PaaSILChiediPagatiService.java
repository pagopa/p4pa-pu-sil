package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPagati;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPagatiRisposta;
import jakarta.activation.DataHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class PaaSILChiediPagatiService extends AbstractQueryPaymentsService<PaaSILChiediPagati, PaaSILChiediPagatiRisposta> {

  private final DebtPositionService debtPositionService;
  private final PagatiMapper pagatiMapper;
  private final SessionIdMapper sessionIdMapper;

  public PaaSILChiediPagatiService(
    OrganizationService organizationService,
    DebtPositionService debtPositionService,
    PagatiMapper pagatiMapper,
    SessionIdMapper sessionIdMapper) {
    super(organizationService);
    this.debtPositionService = debtPositionService;
    this.pagatiMapper = pagatiMapper;
    this.sessionIdMapper = sessionIdMapper;
  }

  @Override
  protected void validateRequest(PaaSILChiediPagati request) {
    //nothing to do here, no specific validation needed
  }

  @Override
  protected SilFaults getFaultForDebtPositionNotFound() {
    return SilFaults.PAA_ID_SESSION_NON_VALIDO;
  }

  @Override
  protected List<Pair<DebtPositionDTO, InstallmentDTO>> getDebtPositionsAndInstallments(PaaSILChiediPagati request, Organization organization, String accessToken) {
    //idSession is installmentId
    List<Long> installmentIds = sessionIdMapper.mapSessionIdToInstallmentIds(request.getIdSession());

    return installmentIds.stream()
      //search for the debt position by installmentId
      .map(installmentId -> Pair.of(installmentId, debtPositionService.getDebtPositionByInstallmentId(installmentId, accessToken)))
      //find the installment in the debt position
      .map(debtPositionPair -> Pair.of(debtPositionPair.getRight(), findInstallmentOfDebtPosition(debtPositionPair.getRight(),
        installment -> Objects.equals(installment.getInstallmentId(), debtPositionPair.getLeft()))))
      //return the pair of debt position and matching installment
      .toList();
  }

  @Override
  protected String getOrgIpaCode(PaaSILChiediPagati request) {
    return request.getCodIpaEnte();
  }


  @Override
  protected PaaSILChiediPagatiRisposta mapper(List<Pair<DebtPositionDTO, InstallmentDTO>> debtPositionWithInstallmentList,
                                                         Organization organization, String accessToken) {

    //TODO currently support only one debt position and installment, but could be extended to support multiple
    InstallmentDTO installmentDTO = debtPositionWithInstallmentList.getFirst().getRight();

    //map debt position to Pagati
    byte[] encodedPagati = pagatiMapper.mapDebtPositionsToEncodedPagati(installmentDTO, organization, accessToken);

    // Prepare the response
    PaaSILChiediPagatiRisposta response = new PaaSILChiediPagatiRisposta();
    response.setPagati(new DataHandler(new ByteArrayDataSource("application/octet-stream", encodedPagati)));
    return response;
  }
}
