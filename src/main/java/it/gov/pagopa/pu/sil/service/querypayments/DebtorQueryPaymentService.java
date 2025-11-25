package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.PaymentHistoryDTO;
import it.gov.pagopa.pu.sil.dto.generated.PaymentHistoryResponseDTO;
import it.gov.pagopa.pu.sil.mapper.ReceiptMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.querypayments.AbstractDebtorQueryPaymentService.DebtorQueryPaymentRequest;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class DebtorQueryPaymentService extends AbstractDebtorQueryPaymentService<DebtorQueryPaymentRequest, PaymentHistoryResponseDTO> {
  private final ReceiptService receiptService;
  private final ReceiptMapper receiptMapper;

  protected DebtorQueryPaymentService(@Value("${public-base-url.bff}") String bffBaseUrl,
                                      DebtPositionService debtPositionService,
                                      OrganizationService organizationService,
                                      AuthorizationService authorizationService,
                                      ReceiptService receiptService,
                                      ReceiptMapper receiptMapper) {
    super(bffBaseUrl, debtPositionService, organizationService, authorizationService);
    this.receiptService = receiptService;
    this.receiptMapper = receiptMapper;
  }

  @Override
  protected DebtorQueryPaymentRequest transformRequest(DebtorQueryPaymentRequest request) {
    OffsetDateTime dateFrom = Optional.ofNullable(request.getDateFrom())
      .orElseGet(() -> OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS));
    OffsetDateTime dateTo = Optional.ofNullable(request.getDateTo())
      .orElseGet(() -> dateFrom.plusDays(1));
    return new DebtorQueryPaymentRequest(request.getIpaCode(),
      request.getDebtorEntityType(),
      request.getDebtorFiscalCode(),
      request.getStatus(),
      dateFrom,
      dateTo);
  }

  @Override
  protected PaymentHistoryResponseDTO gatherToResponse(DebtorQueryPaymentRequest request,
                                                       List<Organization> organizations,
                                                       List<DebtPositionDTO> debtPositions,
                                                       String accessToken) {
    PaymentHistoryResponseDTO response = new PaymentHistoryResponseDTO();
    response.setDateTo(request.getDateTo());

    debtPositions.stream()
      .flatMap(debtPosition -> {
        Organization organization = organizations.stream()
          .filter(org -> Objects.equals(org.getOrganizationId(), debtPosition.getOrganizationId()))
          .findFirst()
          .orElseThrow();

        return debtPosition.getPaymentOptions().stream()
          .flatMap(paymentOption -> paymentOption.getInstallments().stream()
            .map(installment -> PaymentHistoryDTO.builder()
              .ipaCode(organization.getIpaCode())
              .orgName(organization.getOrgName())
              .receipt(receiptMapper.map2ReceiptWithAdditionalNodeDataDTO(installment, accessToken))
              .receiptBytes(receiptService.getReceiptById(installment.getReceiptId(), organization.getOrganizationId(), accessToken))
              .receiptDownloadUrl(composeReceiptDownloadUrl(organization.getOrganizationId(), installment.getReceiptId(), accessToken))
              .build()));
      })
      .forEach(response::addPaymentsItem);

    return response;
  }
}
