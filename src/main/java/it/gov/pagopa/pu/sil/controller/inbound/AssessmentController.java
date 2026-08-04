package it.gov.pagopa.pu.sil.controller.inbound;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.controller.generated.AssessmentApi;
import it.gov.pagopa.pu.sil.dto.generated.GetAssessmentResponseDTO;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.inbound.payments.queryassessments.QueryAssessmentsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class AssessmentController implements AssessmentApi {
  private final QueryAssessmentsService queryAssessmentsService;

  public AssessmentController(QueryAssessmentsService queryAssessmentsService) {
    this.queryAssessmentsService = queryAssessmentsService;
  }

  @Override
  public ResponseEntity<GetAssessmentResponseDTO> getAssessmentByBill(String orgFiscalCode, String billYear, String billNumber) {
    log.info("Received request to get assessment by bill for orgFiscalCode: {}, billYear: {}, billNumber: {}", orgFiscalCode, billYear, billNumber);
    String accessToken = SecurityUtils.getAccessToken();
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode);
    return ResponseEntity.ok(queryAssessmentsService.getAssessment(
        userInfo,
        accessToken,
        orgIpaCode,
        null,
        billYear,
        billNumber
    ));
  }

  @Override
  public ResponseEntity<GetAssessmentResponseDTO> getAssessmentByPaymentReporting(String orgFiscalCode, String iuf) {
    log.info("Received request to get assessment by payment reporting for orgFiscalCode: {}, iuf: {}", orgFiscalCode, iuf);
    String accessToken = SecurityUtils.getAccessToken();
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode);
    return ResponseEntity.ok(queryAssessmentsService.getAssessment(
        userInfo,
        accessToken,
        orgIpaCode,
        iuf,
        null,
        null
    ));
  }
}
