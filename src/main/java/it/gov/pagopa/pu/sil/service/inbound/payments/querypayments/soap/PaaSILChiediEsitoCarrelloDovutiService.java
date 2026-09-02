package it.gov.pagopa.pu.sil.service.inbound.payments.querypayments.soap;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.QueryPaymentStatusType;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.legacy.CartStatus;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.service.inbound.payments.debtposition.DebtPositionInstallmentFacadeService;
import it.gov.pagopa.pu.sil.service.inbound.payments.querypayments.AbstractQueryPaymentsService;
import it.gov.pagopa.pu.sil.service.inbound.payments.querypayments.PaymentStatusRequest;
import it.gov.pagopa.pu.sil.service.inbound.payments.receipt.ReceiptService;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.veneto.regione.pagamenti.ente.ListaCarrelli;
import it.veneto.regione.pagamenti.ente.PaaSILChiediEsitoCarrelloDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILChiediEsitoCarrelloDovutiRisposta;
import it.veneto.regione.pagamenti.ente.RispostaCarrello;
import jakarta.activation.DataHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class PaaSILChiediEsitoCarrelloDovutiService extends AbstractQueryPaymentsService<PaaSILChiediEsitoCarrelloDovuti, PaaSILChiediEsitoCarrelloDovutiRisposta> {

  private final PagatiMapper pagatiMapper;
  private final ReceiptService receiptService;

  public PaaSILChiediEsitoCarrelloDovutiService(
    OrganizationService organizationService,
    DebtPositionInstallmentFacadeService debtPositionInstallmentFacadeService,
    PagatiMapper pagatiMapper,
    ReceiptService receiptService) {
    super(organizationService, debtPositionInstallmentFacadeService);
    this.pagatiMapper = pagatiMapper;
    this.receiptService = receiptService;
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
                                                           Organization organization, String accessToken, PaaSILChiediEsitoCarrelloDovuti request) {
    // Prepare the response
    PaaSILChiediEsitoCarrelloDovutiRisposta response = new PaaSILChiediEsitoCarrelloDovutiRisposta();
    response.setListaCarrelli(new ListaCarrelli());

    // Loop through all installments instead of taking only the first
    for (Pair<DebtPositionDTO, InstallmentDTO> pair : debtPositionWithInstallmentList) {
      InstallmentDTO installmentDTO = pair.getRight();

      RispostaCarrello rispostaCarrello = new RispostaCarrello();

      CartStatus cartStatus = getCartStatus(installmentDTO);
      rispostaCarrello.setEsito(cartStatus.getValue());
      rispostaCarrello.setCodIpaEnte(organization.getIpaCode());

      if(cartStatus == CartStatus.PAID) {
        // Map each installment to Pagati
        byte[] encodedPagati = pagatiMapper.mapDebtPositionsToEncodedPagatiConRicevuta(
          installmentDTO, organization, accessToken);

        // Retrieve the receipt for this specific installment
        byte[] encodedReceipt = receiptService.getReceiptById(
          installmentDTO.getReceiptId(), organization.getOrganizationId(), accessToken);

        rispostaCarrello.setPagati(new DataHandler(new ByteArrayDataSource("application/octet-stream", encodedPagati)));
        rispostaCarrello.setRt(new DataHandler(new ByteArrayDataSource("application/octet-stream", encodedReceipt)));
      }

      // Add each cart response to the list
      response.getListaCarrelli().getRispostaCarrellos().add(rispostaCarrello);
    }
    return response;
  }

  @Override
  protected PaymentStatusRequest validateAndTransformRequest(PaaSILChiediEsitoCarrelloDovuti request, String orgIpaCode) {
    if(StringUtils.isBlank(request.getIdSessionCarrello())) {
      throw new SilFaultException(SilFaults.PAA_ID_SESSION_NON_VALIDO, "Errore, è obbligatorio specificare un idSessionCarrello.");
    }
    return new PaymentStatusRequest(orgIpaCode, QueryPaymentStatusType.INSTALLMENT_ID, request.getIdSessionCarrello(), false);
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
