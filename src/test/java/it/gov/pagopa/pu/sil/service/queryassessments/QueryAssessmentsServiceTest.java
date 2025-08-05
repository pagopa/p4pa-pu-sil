package it.gov.pagopa.pu.sil.service.queryassessments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.classification.dto.generated.PaymentsReporting;
import it.gov.pagopa.pu.classification.dto.generated.Treasury;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentNoPII;
import it.gov.pagopa.pu.sil.connector.classification.ClassificationService;
import it.gov.pagopa.pu.sil.connector.debtpositions.InstallmentService;
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
import uk.co.jemos.podam.api.PodamFactory;
import it.gov.pagopa.pu.sil.util.TestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryAssessmentsServiceTest {
  @Mock
  private ClassificationService classificationService;
  @Mock
  private InstallmentService installmentService;
  @Mock
  private AssessmentsBalanceMapper assessmentsBalanceMapper;

  private QueryAssessmentsService service;
  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    service = new QueryAssessmentsService(classificationService, installmentService, assessmentsBalanceMapper);
  }

  @Test
  void givenBillDataWhenGetAssessmentThenNoTreasuryThenThrowAssessmentNotFoundException() {
    // Given
    String orgIpaCode = "org";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);

    when(classificationService.findTreasuryBySemanticKey(anyLong(), any(), any(), any()))
      .thenReturn(Optional.empty());

    // When Then
    assertThrows(AssessmentNotFoundException.class, () ->
      service.getAssessment(userInfo, "token", orgIpaCode, null, "2024", "123")
    );
  }

  @Test
  void givenIufWhenGetAssessmentThenNoPaymentsReportingThenThrowAssessmentNotFoundException() {
    // Given
    String orgIpaCode = "org";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);

    when(classificationService.findPaymentsReportingByOrganizationIdAndIuf(anyLong(), any(), any()))
      .thenReturn(Collections.emptyList());

    // When Then
    assertThrows(AssessmentNotFoundException.class, () ->
      service.getAssessment(userInfo, "token", orgIpaCode, "IUF123", null, null)
    );
  }

  @ParameterizedTest
  @MethodSource("provideGetAssessmentInput")
  void givenBillDataWhenGetAssessmentThenSuccess(String iuf, String billYear, String billNumber) {
    // Given
    String orgIpaCode = "org";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    PaymentsReporting paymentsReporting = podamFactory.manufacturePojo(PaymentsReporting.class);
    InstallmentNoPII installmentNoPII = podamFactory.manufacturePojo(InstallmentNoPII.class);
    AssessmentsBalanceView assessmentsBalanceView = new AssessmentsBalanceView()
      .officeCode("OFF1")
      .debtPositionTypeOrgCode("TYP1")
      .assessmentCode("CAP1")
      .sectionCode("SEC1")
      .amountCents(12345L);
    BalanceDTO balanceDTO = new BalanceDTO();

    if (iuf == null) {
      Treasury treasury = new Treasury();
      treasury.setIuf("iuf123");
      when(classificationService.findTreasuryBySemanticKey(anyLong(), any(), any(), any())).thenReturn(Optional.of(treasury));
    }
    List<PaymentsReporting> paymentsReportingList = new ArrayList<>();
    paymentsReportingList.add(paymentsReporting);
    paymentsReportingList.add(null);
    when(classificationService.findPaymentsReportingByOrganizationIdAndIuf(anyLong(), any(), any())).thenReturn(paymentsReportingList);
    when(installmentService.findAuthorizedByTransferSemanticKey(anyLong(), any(), any(), anyInt(), any(), any())).thenReturn(installmentNoPII);
    when(classificationService.findClosedAssessmentsBalanceViewByOrganizationIdAndIuds(anyLong(), anyList(), any())).thenReturn(List.of(assessmentsBalanceView));
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
