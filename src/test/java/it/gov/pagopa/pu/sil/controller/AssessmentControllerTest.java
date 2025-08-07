package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.dto.generated.GetAssessmentResponseDTO;
import it.gov.pagopa.pu.sil.security.SecurityUtilsTest;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.queryassessments.QueryAssessmentsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentControllerTest {
  @Mock
  private QueryAssessmentsService queryAssessmentsServiceMock;

  @InjectMocks
  private AssessmentController controller;

  private final String accessToken = "fakeAccessToken";
  private final String orgFiscalCode = "ORG123456789";
  private final String orgIpaCode = "userIpaCode";
  private UserInfo userInfo;

  @BeforeEach
  void setUp() {
    userInfo = AuthorizationServiceTest.buildAdminUser(1L, orgFiscalCode, orgIpaCode);
    userInfo.setMappedExternalUserId("fakeExternalUser");
    SecurityUtilsTest.configureSecurityContext(accessToken, userInfo);
  }

  @AfterEach
  void clear(){
    SecurityUtilsTest.clearSecurityContext();
  }

  @Test
  void whenGetAssessmentByBillThenOk() {
    // Given
    Integer billYear = 2024;
    String billNumber = "12345";
    GetAssessmentResponseDTO expectedResult = new GetAssessmentResponseDTO();

    when(queryAssessmentsServiceMock.getAssessment(
        userInfo,
        accessToken,
        orgIpaCode,
        null,
        billYear.toString(),
        billNumber
    )).thenReturn(expectedResult);
    // When
    ResponseEntity<GetAssessmentResponseDTO> response = controller.getAssessmentByBill(orgFiscalCode, billYear, billNumber);

    // Then
    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertSame(expectedResult, response.getBody());
  }

  @Test
  void whenGetAssessmentByPaymentReportingThenOk() {
    // Given
    String iuf = "IUF12345";
    GetAssessmentResponseDTO expectedResult = new GetAssessmentResponseDTO();

    when(queryAssessmentsServiceMock.getAssessment(
        userInfo,
        accessToken,
        orgIpaCode,
        iuf,
        null,
        null
    )).thenReturn(expectedResult);

    // When
    ResponseEntity<GetAssessmentResponseDTO> response = controller.getAssessmentByPaymentReporting(orgFiscalCode, iuf);

    // Then
    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertSame(expectedResult, response.getBody());
  }
}
