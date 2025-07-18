package it.gov.pagopa.pu.sil.service.queryassessments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.classification.dto.generated.PaymentsReporting;
import it.gov.pagopa.pu.classification.dto.generated.Treasury;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentNoPII;
import it.gov.pagopa.pu.sil.connector.classification.ClassificationService;
import it.gov.pagopa.pu.sil.connector.debtpositions.InstallmentService;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.AssessmentsBalanceMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.veneto.regione.pagamenti.pivot.ente.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryAssessmentsServiceTest {
  @Mock
  private ClassificationService classificationServiceMock;
  @Mock
  private InstallmentService installmentServiceMock;
  @Mock
  private AssessmentsBalanceMapper assessmentsBalanceMapperMock;

  private QueryAssessmentsService service;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    service = new QueryAssessmentsService(classificationServiceMock, installmentServiceMock, assessmentsBalanceMapperMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      classificationServiceMock,
      installmentServiceMock,
      assessmentsBalanceMapperMock
    );
  }

  @Test
  void givenUserNotAdminWhenHandlePivotSILChiediAccertamentoThenUnauthorizedException() {
    // Given
    String orgIpaCode = "org";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", "OTHERIPACODE");
    PivotSILChiediAccertamento request = mock(PivotSILChiediAccertamento.class);

    // When, Then
    assertThrows(AuthorizationDeniedException.class, () ->
      service.handlePivotSILChiediAccertamento(userInfo, "token", orgIpaCode, request)
    );
  }

  @Test
  void givenFullRequestWhenHandlePivotSILChiediAccertamentoThenIllegalArgumentException() {
    // Given
    String orgIpaCode = "org";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    PivotSILChiediAccertamento request = new PivotSILChiediAccertamento();
    RichiestaPerBolletta richiestaPerBolletta = mock(RichiestaPerBolletta.class);
    RichiestaPerIUF richiestaPerIUF = mock(RichiestaPerIUF.class);
    request.setRichiestaPerIUF(richiestaPerIUF);
    request.setRichiestaPerBolletta(richiestaPerBolletta);
    try (MockedStatic<ValidationUtils> validationMock = mockStatic(ValidationUtils.class)) {
      validationMock.when(() -> ValidationUtils.verifyExclusivePresence(request.getRichiestaPerBolletta(), request.getRichiestaPerIUF()))
        .thenReturn(false);

      // When, Then
      assertThrows(IllegalArgumentException.class, () ->
        service.handlePivotSILChiediAccertamento(userInfo, "token", "org", request)
      );
    }
  }

  @Test
  void givenRichiestaPerBollettaWhenHandlePivotSILChiediAccertamentoThenSilFaultException() {
    // Given
    String orgIpaCode = "org";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    PivotSILChiediAccertamento request = new PivotSILChiediAccertamento();
    RichiestaPerBolletta richiestaPerBolletta = mock(RichiestaPerBolletta.class);
    request.setRichiestaPerBolletta(richiestaPerBolletta);
    try (MockedStatic<ValidationUtils> validationMock = mockStatic(ValidationUtils.class)) {
      validationMock.when(() -> ValidationUtils.verifyExclusivePresence(request.getRichiestaPerBolletta(), request.getRichiestaPerIUF()))
        .thenReturn(true);

      when(classificationServiceMock.findTreasuryBySemanticKey(anyLong(), any(), any(), any()))
        .thenReturn(Optional.empty());

      // When, Then
      assertThrows(SilFaultException.class, () ->
        service.handlePivotSILChiediAccertamento(userInfo, "token", "org", request)
      );
    }
  }

  @Test
  void givenRichiestaPerIUFWhenHandlePivotSILChiediAccertamentoThenSilFaultException() {
    // Ginve
    String orgIpaCode = "org";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    PivotSILChiediAccertamento request = new PivotSILChiediAccertamento();
    RichiestaPerIUF richiestaPerIUF = mock(RichiestaPerIUF.class);
    request.setRichiestaPerIUF(richiestaPerIUF);
    try (MockedStatic<ValidationUtils> validationMock = mockStatic(ValidationUtils.class)) {
      validationMock.when(() -> ValidationUtils.verifyExclusivePresence(request.getRichiestaPerBolletta(), request.getRichiestaPerIUF()))
        .thenReturn(true);

      when(classificationServiceMock.findPaymentsReportingByOrganizationIdAndIuf(anyLong(), any(), any()))
        .thenReturn(Collections.emptyList());

      // When, Then
      assertThrows(SilFaultException.class, () ->
        service.handlePivotSILChiediAccertamento(userInfo, "token", "org", request)
      );
    }
  }

  @Test
  void givenRichiestaPerBollettaWhenHandlePivotSILChiediAccertamentoThenSuccess() {
    // Given
    String orgIpaCode = "org";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    PivotSILChiediAccertamento request = new PivotSILChiediAccertamento();
    RichiestaPerBolletta richiestaPerBolletta = podamFactory.manufacturePojo(RichiestaPerBolletta.class);
    request.setRichiestaPerBolletta(richiestaPerBolletta);
    Treasury treasury = mock(Treasury.class);
    treasury.setIuf("iuf");
    AssessmentsBalanceView assessmentsBalanceView = new AssessmentsBalanceView()
      .officeCode("OFF1")
      .debtPositionTypeOrgCode("TYP1")
      .assessmentCode("CAP1")
      .sectionCode("SEC1")
      .amountCents(12345L);
    PaymentsReporting paymentsReporting = podamFactory.manufacturePojo(PaymentsReporting.class);
    CtBilancio expectedCtBilancio = new CtBilancio();
    InstallmentNoPII installmentNoPII = podamFactory.manufacturePojo(InstallmentNoPII.class);
    try (MockedStatic<ValidationUtils> validationMock = mockStatic(ValidationUtils.class)) {
      validationMock.when(() -> ValidationUtils.verifyExclusivePresence(request.getRichiestaPerBolletta(), request.getRichiestaPerIUF()))
        .thenReturn(true);

      when(classificationServiceMock.findTreasuryBySemanticKey(anyLong(), any(), any(), any()))
        .thenReturn(Optional.of(treasury));
      when(classificationServiceMock.findPaymentsReportingByOrganizationIdAndIuf(anyLong(), any(), any()))
        .thenReturn(List.of(paymentsReporting));
      when(installmentServiceMock.findAuthorizedByTransferSemanticKey(anyLong(), any(), any(), anyInt(), any(), any()))
        .thenReturn(installmentNoPII);
      when(classificationServiceMock.findClosedAssessmentsBalanceViewByOrganizationIdAndIuds(anyLong(), anyList(), any()))
        .thenReturn(List.of(assessmentsBalanceView));
      when(assessmentsBalanceMapperMock.map2CtBilancio(assessmentsBalanceView)).thenReturn(expectedCtBilancio);

      // When
      PivotSILChiediAccertamentoRisposta resp = service.handlePivotSILChiediAccertamento(userInfo, "token", "org", request);

      // Then
      assertNotNull(resp);
      assertEquals(expectedCtBilancio, resp.getBilancios().getFirst());
    }
  }
}
