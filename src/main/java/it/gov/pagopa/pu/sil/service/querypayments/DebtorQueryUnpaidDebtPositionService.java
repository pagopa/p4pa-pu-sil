package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.UnpaidDebtPositionsDTO;
import it.gov.pagopa.pu.sil.dto.generated.UnpaidDebtPositionsResponseDTO;
import it.gov.pagopa.pu.sil.mapper.PaymentMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionCheckoutService;
import it.gov.pagopa.pu.sil.service.querypayments.AbstractDebtorQueryPaymentService.DebtorQueryPaymentRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DebtorQueryUnpaidDebtPositionService extends AbstractDebtorQueryPaymentService<DebtorQueryPaymentRequest, UnpaidDebtPositionsResponseDTO> {

  private final PaymentMapper paymentMapper;

  protected DebtorQueryUnpaidDebtPositionService(@Value("${public-base-url.bff}") String bffBaseUrl,
                                                 DebtPositionService debtPositionService,
                                                 OrganizationService organizationService,
                                                 AuthorizationService authorizationService,
                                                 DebtPositionCheckoutService debtPositionCheckoutService,
                                                 PaymentMapper paymentMapper) {
    super(bffBaseUrl, debtPositionService, organizationService, authorizationService, debtPositionCheckoutService);
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
            .filter(installment -> InstallmentStatus.UNPAID.equals(installment.getStatus()))
            .map(installment -> UnpaidDebtPositionsDTO.builder()
                .ipaCode(organization.getIpaCode())
                .orgName(organization.getOrgName())
              .paymentTriggerUrl(
                getCheckoutUrl(organization.getOrganizationId(),
                  installment.getIuv(), null, organization.getOrgFiscalCode(),
                  accessToken))
                .unpaidDebtPosition(paymentMapper.mapToPaymentDTO(installment, accessToken))
              .build()));
      })
      .forEach(response::addDebtPositionsItem);

    return response;
  }
}
