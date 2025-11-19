package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.client.CheckoutClient;
import it.gov.pagopa.pu.sil.dto.generated.UnpaidDebtPositionsDTO;
import it.gov.pagopa.pu.sil.dto.generated.UnpaidDebtPositionsResponseDTO;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.mapper.PaymentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class DebtorQueryUnpaidDebtPositionService extends AbstractDebtorQueryPaymentService<DebtorQueryPaymentRequest, UnpaidDebtPositionsResponseDTO> {

  private final CheckoutClient checkoutClient;
  private final CartRequestMapper cartRequestMapper;
  private final PaymentMapper paymentMapper;

  protected DebtorQueryUnpaidDebtPositionService(@Value("${public-base-url.fileshare}") String fileShareBaseUrl,
                                                 DebtPositionService debtPositionService,
                                                 OrganizationService organizationService,
                                                 CheckoutClient checkoutClient,
                                                 CartRequestMapper cartRequestMapper,
                                                 PaymentMapper paymentMapper) {
    super(fileShareBaseUrl, debtPositionService, organizationService);
    this.checkoutClient = checkoutClient;
    this.cartRequestMapper = cartRequestMapper;
    this.paymentMapper = paymentMapper;
  }

  @Override
  protected String getCodIpaEnte(DebtorQueryPaymentRequest request) {
    return request.ipaCode();
  }

  @Override
  protected String getDebtorFiscalCode(DebtorQueryPaymentRequest request) {
    return request.debtorFiscalCode();
  }

  @Override
  protected PersonEntityType getDebtorEntityType(DebtorQueryPaymentRequest request) {
    return request.debtorEntityType();
  }

  @Override
  protected InstallmentStatus getInstallmentStatus() {
    return InstallmentStatus.UNPAID;
  }

  @Override
  protected OffsetDateTimeIntervalFilter getDateFilter(DebtorQueryPaymentRequest debtorQueryPaymentRequest) {
    return new OffsetDateTimeIntervalFilter(null, null);
  }

  @Override
  protected UnpaidDebtPositionsResponseDTO createResponse() {
    return new UnpaidDebtPositionsResponseDTO();
  }

  @Override
  protected void gatherToResponse(DebtorQueryPaymentRequest debtorQueryPaymentRequest, Organization organization, List<DebtPositionDTO> debtPositions, String accessToken, UnpaidDebtPositionsResponseDTO response) {
    debtPositions.stream()
      .flatMap(debtPosition -> debtPosition.getPaymentOptions().stream())
      .flatMap(paymentOption -> paymentOption.getInstallments().stream())
      .map(installment -> {
        String cartId = UUID.randomUUID().toString();
        CartRequest cartRequest = cartRequestMapper.mapInstallmentToCartRequest(installment, organization, cartId, null);
        String checkoutCart = checkoutClient.checkoutCart(cartRequest);
        return UnpaidDebtPositionsDTO.builder()
          .ipaCode(organization.getIpaCode())
          .orgName(organization.getOrgName())
          .paymentTriggerUrl(composeCheckoutUrl(checkoutCart)) // @TODO: da implementare con la P4ADEV-4043
          .unpaidDebtPosition(paymentMapper.mapToPaymentDTO(installment, accessToken))
          .build();
      })
      .forEach(response::addDebtPositionsItem);
  }
}
