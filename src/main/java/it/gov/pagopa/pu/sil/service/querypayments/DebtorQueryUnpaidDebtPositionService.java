package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.client.CheckoutClient;
import it.gov.pagopa.pu.sil.dto.generated.UnpaidDebtPositionsDTO;
import it.gov.pagopa.pu.sil.dto.generated.UnpaidDebtPositionsResponseDTO;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.mapper.PaymentMapper;
import it.gov.pagopa.pu.sil.service.querypayments.AbstractDebtorQueryPaymentService.DebtorQueryPaymentRequest;
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
  protected DebtorQueryPaymentRequest transformRequest(DebtorQueryPaymentRequest request) {
    // Request is already in the correct format, just ensure status is UNPAID
    return new DebtorQueryPaymentRequest(
      request.getIpaCode(),
      request.getDebtorEntityType(),
      request.getDebtorFiscalCode(),
      InstallmentStatus.UNPAID,
      null,
      null
    );
  }

  @Override
  protected UnpaidDebtPositionsResponseDTO gatherToResponse(DebtorQueryPaymentRequest request,
                                                            List<Organization> organizations,
                                                            List<DebtPositionDTO> debtPositions,
                                                            String accessToken) {
    UnpaidDebtPositionsResponseDTO response = new UnpaidDebtPositionsResponseDTO();

    debtPositions.stream()
      .flatMap(debtPosition -> {
        Organization organization = organizations.stream()
          .filter(org -> org.getOrganizationId().equals(debtPosition.getOrganizationId()))
          .findFirst()
          .orElseThrow();

        return debtPosition.getPaymentOptions().stream()
          .flatMap(paymentOption -> paymentOption.getInstallments().stream()
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
            }));
      })
      .forEach(response::addDebtPositionsItem);

    return response;
  }
}
