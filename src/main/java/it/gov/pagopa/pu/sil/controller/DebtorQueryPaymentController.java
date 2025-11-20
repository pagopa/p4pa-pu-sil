package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.sil.controller.generated.DebtorQueryPaymentApi;
import it.gov.pagopa.pu.sil.dto.generated.PaymentHistoryResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.UnpaidDebtPositionsResponseDTO;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.querypayments.AbstractDebtorQueryPaymentService.DebtorQueryPaymentRequest;
import it.gov.pagopa.pu.sil.service.querypayments.DebtorQueryPaymentService;
import it.gov.pagopa.pu.sil.service.querypayments.DebtorQueryUnpaidDebtPositionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@Slf4j
@RestController
public class DebtorQueryPaymentController implements DebtorQueryPaymentApi {
  private final DebtorQueryPaymentService debtorQueryPaymentService;
  private final DebtorQueryUnpaidDebtPositionService debtorQueryUnpaidDebtPositionService;

  public DebtorQueryPaymentController(DebtorQueryPaymentService debtorQueryPaymentService,
                                      DebtorQueryUnpaidDebtPositionService debtorQueryUnpaidDebtPositionService) {
    this.debtorQueryPaymentService = debtorQueryPaymentService;
    this.debtorQueryUnpaidDebtPositionService = debtorQueryUnpaidDebtPositionService;
  }

  @Override
  public ResponseEntity<PaymentHistoryResponseDTO> getPaymentHistory(String debtorFiscalCode, PersonEntityType debtorEntityType, OffsetDateTime dateFrom, OffsetDateTime dateTo, String ipaCode) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    DebtorQueryPaymentRequest request = new DebtorQueryPaymentRequest(
      ipaCode,
      debtorEntityType,
      debtorFiscalCode,
      InstallmentStatus.PAID,
      dateFrom,
      dateTo
    );

    return ResponseEntity.ok(debtorQueryPaymentService.processRequest(request, userInfo, accessToken));
  }

  @Override
  public ResponseEntity<UnpaidDebtPositionsResponseDTO> getUnpaidDebtPositions(String debtorFiscalCode, PersonEntityType debtorEntityType, String ipaCode) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    DebtorQueryPaymentRequest request = new DebtorQueryPaymentRequest(
      ipaCode,
      debtorEntityType,
      debtorFiscalCode,
      InstallmentStatus.UNPAID,
      null,
      null
    );

    return ResponseEntity.ok(debtorQueryUnpaidDebtPositionService.processRequest(request, userInfo, accessToken));
  }
}
