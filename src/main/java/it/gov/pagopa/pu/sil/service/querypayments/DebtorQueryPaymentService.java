package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.PaymentHistoryDTO;
import it.gov.pagopa.pu.sil.dto.generated.PaymentHistoryResponseDTO;
import it.gov.pagopa.pu.sil.mapper.ReceiptMapper;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class DebtorQueryPaymentService extends AbstractDebtorQueryPaymentService<DebtorQueryPaymentRequest, PaymentHistoryResponseDTO> {
  private final ReceiptService receiptService;
  private final ReceiptMapper receiptMapper;

  protected DebtorQueryPaymentService(@Value("${public-base-url.fileshare}") String fileShareBaseUrl,
                                      DebtPositionService debtPositionService,
                                      OrganizationService organizationService,
                                      ReceiptService receiptService,
                                      ReceiptMapper receiptMapper) {
    super(fileShareBaseUrl, debtPositionService, organizationService);
    this.receiptService = receiptService;
    this.receiptMapper = receiptMapper;
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
    return InstallmentStatus.PAID;
  }

  @Override
  protected OffsetDateTimeIntervalFilter getDateFilter(DebtorQueryPaymentRequest request) {
    OffsetDateTime dateFrom = Optional.ofNullable(request.dateFrom())
        .orElseGet(() -> OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS));
    OffsetDateTime dateTo = Optional.ofNullable(request.dateTo())
        .orElseGet(() -> dateFrom.plusDays(1));
    return new OffsetDateTimeIntervalFilter(dateFrom, dateTo);
  }

  @Override
  protected PaymentHistoryResponseDTO createResponse() {
    return new PaymentHistoryResponseDTO();
  }

  @Override
  protected void gatherToResponse(DebtorQueryPaymentRequest request, Organization organization, List<DebtPositionDTO> debtPositions, String accessToken, PaymentHistoryResponseDTO response) {
    OffsetDateTimeIntervalFilter dateFilter = getDateFilter(request);
    response.setDateTo(dateFilter.getTo());
    debtPositions.stream()
      .flatMap(debtPosition -> debtPosition.getPaymentOptions().stream())
      .flatMap(paymentOption -> paymentOption.getInstallments().stream())
      .map(installment -> PaymentHistoryDTO.builder()
        .ipaCode(organization.getIpaCode())
        .orgName(organization.getOrgName())
        .receipt(receiptMapper.map2ReceiptWithAdditionalNodeDataDTO(installment, accessToken))
        .receiptBytes(receiptService.getReceiptById(installment.getReceiptId(), organization.getOrganizationId(), accessToken))
        .receiptDownloadUrl(composeReceiptDownloadUrl(organization.getOrganizationId(), installment.getReceiptId()))
        .build())
      .forEach(response::addPaymentsItem);
  }
}
