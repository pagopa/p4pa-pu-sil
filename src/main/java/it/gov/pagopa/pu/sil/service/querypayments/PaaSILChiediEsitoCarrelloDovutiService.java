package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.legacy.CartStatus;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.veneto.regione.pagamenti.ente.ListaCarrelli;
import it.veneto.regione.pagamenti.ente.PaaSILChiediEsitoCarrelloDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILChiediEsitoCarrelloDovutiRisposta;
import it.veneto.regione.pagamenti.ente.RispostaCarrello;
import jakarta.activation.DataHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class PaaSILChiediEsitoCarrelloDovutiService extends AbstractQueryPaymentsService<PaaSILChiediEsitoCarrelloDovuti, PaaSILChiediEsitoCarrelloDovutiRisposta> {

  private final DebtPositionService debtPositionService;
  private final PagatiMapper pagatiMapper;
  private final SessionIdMapper sessionIdMapper;
  private final ReceiptService receiptService;

  public PaaSILChiediEsitoCarrelloDovutiService(
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
  protected void validateRequest(PaaSILChiediEsitoCarrelloDovuti request) {
    //nothing to do here, no specific validation needed
  }

  @Override
  protected SilFaults getFaultForDebtPositionNotFound() {
    return SilFaults.PAA_ID_SESSION_NON_VALIDO;
  }

  @Override
  protected List<Pair<DebtPositionDTO, InstallmentDTO>> getDebtPositionsAndInstallments(PaaSILChiediEsitoCarrelloDovuti request, Organization organization, String accessToken) {
    //idSession is installmentId
    List<Long> installmentIds = sessionIdMapper.mapSessionIdToInstallmentIds(request.getIdSessionCarrello());

    return installmentIds.stream()
      //search for the debt position by installmentId
      .map(installmentId -> Pair.of(installmentId, debtPositionService.getDebtPositionDTOByInstallmentId(installmentId, accessToken)))
      //find the installment in the debt position
      .map(debtPositionPair -> Pair.of(debtPositionPair.getRight(), findInstallmentOfDebtPosition(debtPositionPair.getRight(),
        installment -> Objects.equals(installment.getInstallmentId(), debtPositionPair.getLeft()))))
      //return the pair of debt position and matching installment
      .toList();
  }

  @Override
  protected String getOrgIpaCode(PaaSILChiediEsitoCarrelloDovuti request) {
    return request.getCodIpaEnte();
  }

  @Override
  protected void validateInstallmentStatus(InstallmentDTO installment) {
    //for this API, the result of this validation is not thrown as a fault, but it's returned in the "esito" field of the response
  }

  @Override
  protected PaaSILChiediEsitoCarrelloDovutiRisposta mapper(List<Pair<DebtPositionDTO, InstallmentDTO>> debtPositionWithInstallmentList,
                                                         Organization organization, String accessToken) {

    // Prepare the response
    PaaSILChiediEsitoCarrelloDovutiRisposta response = new PaaSILChiediEsitoCarrelloDovutiRisposta();
    response.setListaCarrelli(new ListaCarrelli());
    RispostaCarrello rispostaCarrello = new RispostaCarrello();
    response.getListaCarrelli().getRispostaCarrellos().add(rispostaCarrello);

    //TODO currently support only one debt position and installment, but could be extended to support multiple
    InstallmentDTO installmentDTO = debtPositionWithInstallmentList.getFirst().getRight();

    CartStatus cartStatus = getCartStatus(installmentDTO);
    rispostaCarrello.setEsito(cartStatus.getValue());
    rispostaCarrello.setCodIpaEnte(organization.getIpaCode());
    if(cartStatus == CartStatus.PAID) {
      //map debt position to Pagati
      byte[] encodedPagati = pagatiMapper.mapDebtPositionsToEncodedPagatiConRicevuta(installmentDTO, organization, accessToken);

      //retrieve the original receipt
      byte[] encodedReceipt = receiptService.getReceiptById(installmentDTO.getReceiptId(), organization.getOrganizationId(), accessToken);

      rispostaCarrello.setPagati(new DataHandler(new ByteArrayDataSource("application/octet-stream", encodedPagati)));
      rispostaCarrello.setRt(new DataHandler(new ByteArrayDataSource("application/octet-stream", encodedReceipt)));
    }

    return response;
  }

  private CartStatus getCartStatus(InstallmentDTO installment) {
    InstallmentStatus status = Objects.equals(installment.getStatus(), InstallmentStatus.TO_SYNC) ? installment.getSyncStatus().getSyncStatusTo() : installment.getStatus();
    //throw fault if installment is not paid
    if(Objects.equals(status, InstallmentStatus.UNPAID)){
      return CartStatus.UNPAID;
    } else if(Objects.equals(status, InstallmentStatus.EXPIRED)){
      return CartStatus.EXPIRED;
    } else if(Objects.equals(status, InstallmentStatus.PAID) || Objects.equals(status, InstallmentStatus.REPORTED)) {
      return CartStatus.PAID;
    } else {
      //any other state
      log.error("Installment with id[{}] has invalid status[{}]", installment.getInstallmentId(), status);
      return CartStatus.NOT_PAYABLE;
    }
  }
}
