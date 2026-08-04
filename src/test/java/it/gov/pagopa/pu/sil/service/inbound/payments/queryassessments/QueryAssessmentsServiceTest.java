package it.gov.pagopa.pu.sil.service.inbound.payments.queryassessments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.sil.connector.classification.AssessmentService;
import it.gov.pagopa.pu.sil.dto.generated.BalanceDTO;
import it.gov.pagopa.pu.sil.dto.generated.GetAssessmentResponseDTO;
import it.gov.pagopa.pu.sil.exception.AssessmentNotFoundException;
import it.gov.pagopa.pu.sil.mapper.AssessmentsBalanceMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryAssessmentsServiceTest {
  @Mock
  private AssessmentService assessmentService;
  @Mock
  private AssessmentsBalanceMapper assessmentsBalanceMapper;

  private QueryAssessmentsService service;

  private UserInfo userInfo;
  private final String orgIpaCode = "ORG_IPA_CODE";

  @BeforeEach
  void setUp() {
    service = new QueryAssessmentsService(assessmentService, assessmentsBalanceMapper);
    userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
  }

  @Test
  void givenUserNotAdminWhenWhenGetAssessmentThenNoTreasuryThenUnauthorizedException() {
    // Given
    String otherOrgIpaCode = "OTHER_ORG_IPA_CODE";
    // When Then
    assertThrows(AuthorizationDeniedException.class, () ->
      service.getAssessment(userInfo, "token", otherOrgIpaCode, null, "2024", "123")
    );
  }

  @Test
  void givenIufWhenGetAssessmentThenNoPaymentsReportingThenThrowAssessmentNotFoundException() {
    // Given
    String iuf = "IUF123";

    when(assessmentService.findClosedAssessmentsBalanceViewByOrganizationIdAndIuf(anyLong(), any(), any()))
      .thenReturn(Collections.emptyList());

    // When Then
    assertThrows(AssessmentNotFoundException.class, () ->
      service.getAssessment(userInfo, "token", orgIpaCode, iuf, null, null)
    );
  }

  @ParameterizedTest
  @MethodSource("provideGetAssessmentInput")
  void givenBillDataWhenGetAssessmentThenSuccess(String iuf, String billYear, String billNumber) {
    // Given
    AssessmentsBalanceView assessmentsBalanceView = new AssessmentsBalanceView()
      .officeCode("OFF1")
      .debtPositionTypeOrgCode("TYP1")
      .assessmentCode("CAP1")
      .sectionCode("SEC1")
      .amountCents(12345L);
    BalanceDTO balanceDTO = new BalanceDTO();

    if (iuf == null) {
      when(assessmentService.findClosedAssessmentsBalanceViewByOrganizationIdAndBill(
          anyLong(), anyString(), anyString(), anyString()))
        .thenReturn(List.of(assessmentsBalanceView));
    } else {
      when(assessmentService.findClosedAssessmentsBalanceViewByOrganizationIdAndIuf(anyLong(), anyString(), anyString()))
        .thenReturn(List.of(assessmentsBalanceView));
    }
    when(assessmentsBalanceMapper.map2BalanceDTO(assessmentsBalanceView)).thenReturn(balanceDTO);

    // When
    GetAssessmentResponseDTO resp = service.getAssessment(userInfo, "token", orgIpaCode, iuf, billYear, billNumber);

    // Then
    assertNotNull(resp);
    assertEquals(1, resp.getBalances().size());
    assertEquals(balanceDTO, resp.getBalances().getFirst());
  }

  private static Stream<Arguments> provideGetAssessmentInput() {
    return Stream.of(
        Arguments.of( null, "2024", "123"),
        Arguments.of("IUF123", null, null)
    );
  }
}
