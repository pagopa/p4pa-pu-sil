package it.gov.pagopa.pu.sil.service.queryassessments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentNoPII;
import it.gov.pagopa.pu.sil.connector.classification.ClassificationService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.mapper.AssessmentsBalanceMapper;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.classification.dto.generated.PaymentsReporting;
import it.gov.pagopa.pu.classification.dto.generated.Treasury;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.veneto.regione.pagamenti.pivot.ente.CtBilancio;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILChiediAccertamento;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILChiediAccertamentoRisposta;
import it.veneto.regione.pagamenti.pivot.ente.RichiestaPerBolletta;
import it.veneto.regione.pagamenti.pivot.ente.RichiestaPerIUF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryAssessmentsServiceTest {
  @Mock
  private ClassificationService classificationService;
  @Mock
  private DebtPositionService debtPositionService;
  @Mock
  private AssessmentsBalanceMapper assessmentsBalanceMapper;

  private QueryAssessmentsService service;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    service = new QueryAssessmentsService(classificationService, debtPositionService, assessmentsBalanceMapper);
  }

  @Test
  void givenUserNotAdminWhenHandlePivotSILChiediAccertamentoThenUnauthorizedException() {
    UserInfo userInfo = mock(UserInfo.class);
    String orgIpaCode = "org";
    PivotSILChiediAccertamento request = mock(PivotSILChiediAccertamento.class);
    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(false);
      assertThrows(UnauthorizedException.class, () ->
        service.handlePivotSILChiediAccertamento(userInfo, "token", orgIpaCode, request)
      );
    }
  }

  @Test
  void givenFullRequestWhenHandlePivotSILChiediAccertamentoThenIllegalArgumentException() {
    UserInfo userInfo = mock(UserInfo.class);
    String orgIpaCode = "org";
    PivotSILChiediAccertamento request = new PivotSILChiediAccertamento();
    RichiestaPerBolletta richiestaPerBolletta = mock(RichiestaPerBolletta.class);
    RichiestaPerIUF richiestaPerIUF = mock(RichiestaPerIUF.class);
    request.setRichiestaPerIUF(richiestaPerIUF);
    request.setRichiestaPerBolletta(richiestaPerBolletta);
    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class);
         MockedStatic<ValidationUtils> validationMock = mockStatic(ValidationUtils.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(isNull(), eq(orgIpaCode))).thenReturn(789L);
      validationMock.when(() -> ValidationUtils.verifyExclusivePresence(request.getRichiestaPerBolletta(), request.getRichiestaPerIUF()))
        .thenReturn(false);

      assertThrows(IllegalArgumentException.class, () ->
        service.handlePivotSILChiediAccertamento(userInfo, "token", "org", request)
      );
    }
  }

  @Test
  void givenRichiestaPerBollettaWhenHandlePivotSILChiediAccertamentoThenSilFaultException() {
    UserInfo userInfo = mock(UserInfo.class);
    String orgIpaCode = "org";
    PivotSILChiediAccertamento request = new PivotSILChiediAccertamento();
    RichiestaPerBolletta richiestaPerBolletta = mock(RichiestaPerBolletta.class);
    request.setRichiestaPerBolletta(richiestaPerBolletta);
    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class);
         MockedStatic<ValidationUtils> validationMock = mockStatic(ValidationUtils.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(isNull(), eq(orgIpaCode))).thenReturn(789L);
      validationMock.when(() -> ValidationUtils.verifyExclusivePresence(request.getRichiestaPerBolletta(), request.getRichiestaPerIUF()))
        .thenReturn(true);

      when(classificationService.findTreasuryBySemanticKey(anyLong(), any(), any(), any()))
        .thenReturn(Optional.empty());
      assertThrows(SilFaultException.class, () ->
        service.handlePivotSILChiediAccertamento(userInfo, "token", "org", request)
      );
    }
  }

  @Test
  void givenRichiestaPerIUFWhenHandlePivotSILChiediAccertamentoThenSilFaultException() {
    UserInfo userInfo = mock(UserInfo.class);
    String orgIpaCode = "org";
    PivotSILChiediAccertamento request = new PivotSILChiediAccertamento();
    RichiestaPerIUF richiestaPerIUF = mock(RichiestaPerIUF.class);
    request.setRichiestaPerIUF(richiestaPerIUF);
    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class);
      MockedStatic<ValidationUtils> validationMock = mockStatic(ValidationUtils.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(isNull(), eq(orgIpaCode))).thenReturn(789L);
      validationMock.when(() -> ValidationUtils.verifyExclusivePresence(request.getRichiestaPerBolletta(), request.getRichiestaPerIUF()))
        .thenReturn(true);

      when(classificationService.findPaymentsReportingByOrganizationIdAndIuf(anyLong(), any(), any()))
        .thenReturn(Collections.emptyList());
      assertThrows(SilFaultException.class, () ->
        service.handlePivotSILChiediAccertamento(userInfo, "token", "org", request)
      );
    }
  }

  @Test
  void givenRichiestaPerBollettaWhenHandlePivotSILChiediAccertamentoThenSuccess() {
    UserInfo userInfo = podamFactory.manufacturePojo(UserInfo.class);
    String orgIpaCode = "org";
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
    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class);
         MockedStatic<ValidationUtils> validationMock = mockStatic(ValidationUtils.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(isNull(), eq(orgIpaCode))).thenReturn(789L);
      validationMock.when(() -> ValidationUtils.verifyExclusivePresence(request.getRichiestaPerBolletta(), request.getRichiestaPerIUF()))
        .thenReturn(true);

      when(classificationService.findTreasuryBySemanticKey(anyLong(), any(), any(), any()))
        .thenReturn(Optional.of(treasury));
      when(classificationService.findPaymentsReportingByOrganizationIdAndIuf(anyLong(), any(), any()))
        .thenReturn(List.of(paymentsReporting));
      when(debtPositionService.findAuthorizedByTransferSemanticKey(anyLong(), any(), any(), anyInt(), any(), any()))
        .thenReturn(installmentNoPII);
      when(classificationService.findClosedAssessmentsBalanceViewByOrganizationIdAndIuds(anyLong(), anyList(), any()))
        .thenReturn(List.of(assessmentsBalanceView));
      when(assessmentsBalanceMapper.map2CtBilancio(assessmentsBalanceView)).thenReturn(expectedCtBilancio);

      PivotSILChiediAccertamentoRisposta resp = service.handlePivotSILChiediAccertamento(userInfo, "token", "org", request);
      assertNotNull(resp);
      assertEquals(expectedCtBilancio, resp.getBilancios().getFirst());
    }
  }
}
