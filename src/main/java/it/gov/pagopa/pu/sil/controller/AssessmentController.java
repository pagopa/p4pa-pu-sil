package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.controller.generated.AssessmentApi;
import it.gov.pagopa.pu.sil.dto.generated.GetAssessmentResponseDTO;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.queryassessments.NativeQueryAssessmentsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class AssessmentController implements AssessmentApi {
  private final NativeQueryAssessmentsService nativeQueryAssessmentsService;

  public AssessmentController(NativeQueryAssessmentsService nativeQueryAssessmentsService) {
    this.nativeQueryAssessmentsService = nativeQueryAssessmentsService;
  }

  @Override
  public ResponseEntity<GetAssessmentResponseDTO> getAssessmentByBill(String orgFiscalCode, Integer billYear, String billNumber) {
    log.info("Received request to get assessment by bill for orgFiscalCode: {}, billYear: {}, billNumber: {}", orgFiscalCode, billYear, billNumber);
    String accessToken = SecurityUtils.getAccessToken();
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    return ResponseEntity.ok(nativeQueryAssessmentsService.getAssessment(
        userInfo,
        accessToken,
        orgFiscalCode,
        null,
        billYear.toString(),
        billNumber
    ));
  }

  @Override
  public ResponseEntity<GetAssessmentResponseDTO> getAssessmentByPaymentReporting(String orgFiscalCode, String iuf) {
    log.info("Received request to get assessment by payment reporting for orgFiscalCode: {}, iuf: {}", orgFiscalCode, iuf);
    String accessToken = SecurityUtils.getAccessToken();
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    return ResponseEntity.ok(nativeQueryAssessmentsService.getAssessment(
        userInfo,
        accessToken,
        orgFiscalCode,
        iuf,
        null,
        null
    ));
  }
}
