package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.sil.controller.generated.DebtorQueryPaymentApi;
import it.gov.pagopa.pu.sil.dto.generated.PaymentHistoryResponseDTO;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.querypayments.DebtorQueryPaymentRequest;
import it.gov.pagopa.pu.sil.service.querypayments.DebtorQueryPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@Slf4j
@RestController
public class DebtorQueryPaymentController implements DebtorQueryPaymentApi {
  private final DebtorQueryPaymentService debtorQueryPaymentService;

  public DebtorQueryPaymentController(DebtorQueryPaymentService debtorQueryPaymentService) {
    this.debtorQueryPaymentService = debtorQueryPaymentService;
  }

  @Override
  public ResponseEntity<PaymentHistoryResponseDTO> getPaymentHistory(String debtorFiscalCode, PersonEntityType debtorEntityType, OffsetDateTime dateFrom, OffsetDateTime dateTo, String ipaCode) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    DebtorQueryPaymentRequest request = new DebtorQueryPaymentRequest(
      ipaCode,
      debtorEntityType,
      debtorFiscalCode,
      dateFrom,
      dateTo
    );

    return ResponseEntity.ok(debtorQueryPaymentService.processRequest(request, userInfo, accessToken));
  }
}
