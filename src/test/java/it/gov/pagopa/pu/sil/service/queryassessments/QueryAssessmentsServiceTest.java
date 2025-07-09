package it.gov.pagopa.pu.sil.service.queryassessments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.sil.connector.classification.ClassificationService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.mapper.AssessmentsBalanceMapper;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.classification.dto.generated.PaymentsReporting;
import it.gov.pagopa.pu.classification.dto.generated.Treasury;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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

  private AssessmentsBalanceMapper assessmentsBalanceMapper = new AssessmentsBalanceMapper();

  private QueryAssessmentsService service;

  @BeforeEach
  void setUp() {
    service = new QueryAssessmentsService(classificationService, debtPositionService, assessmentsBalanceMapper);
  }

  @Test
  void givenUserNotAdminWhenHandlePivotSILChiediAccertamentoThenUnauthorizedException() {
    UserInfo userInfo = Mockito.mock(UserInfo.class);
    String orgIpaCode = "org";
    PivotSILChiediAccertamento request = Mockito.mock(PivotSILChiediAccertamento.class);
    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(false);
      assertThrows(UnauthorizedException.class, () ->
        service.handlePivotSILChiediAccertamento(userInfo, "token", orgIpaCode, request)
      );
    }
  }

  @Test
  void givenFullRequestWhenHandlePivotSILChiediAccertamentoThenIllegalArgumentException() {
    UserInfo userInfo = Mockito.mock(UserInfo.class);
    String orgIpaCode = "org";
    PivotSILChiediAccertamento request = Mockito.mock(PivotSILChiediAccertamento.class);
    RichiestaPerBolletta richiestaPerBolletta = Mockito.mock(RichiestaPerBolletta.class);
    RichiestaPerIUF richiestaPerIUF = Mockito.mock(RichiestaPerIUF.class);
    request.setRichiestaPerIUF(richiestaPerIUF);
    request.setRichiestaPerBolletta(richiestaPerBolletta);
    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class);
         MockedStatic<ValidationUtils> validationMock = mockStatic(ValidationUtils.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(isNull(), eq(orgIpaCode))).thenReturn(789L);
      validationMock.when(() -> ValidationUtils.verifyExclusivePresence(richiestaPerBolletta, richiestaPerIUF))
        .thenReturn(false);

      assertThrows(IllegalArgumentException.class, () ->
        service.handlePivotSILChiediAccertamento(userInfo, "token", "org", request)
      );
    }
  }

  @Test
  void givenRichiestaPerBollettaWhenHandlePivotSILChiediAccertamentoThenSilFaultException() {
    UserInfo userInfo = Mockito.mock(UserInfo.class);
    String orgIpaCode = "org";
    PivotSILChiediAccertamento request = Mockito.mock(PivotSILChiediAccertamento.class);
    RichiestaPerBolletta richiestaPerBolletta = Mockito.mock(RichiestaPerBolletta.class);
    request.setRichiestaPerBolletta(richiestaPerBolletta);
    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class);
         MockedStatic<ValidationUtils> validationMock = mockStatic(ValidationUtils.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(isNull(), eq(orgIpaCode))).thenReturn(789L);
      validationMock.when(() -> ValidationUtils.verifyExclusivePresence(richiestaPerBolletta, isNull()))
        .thenReturn(true);

      Mockito.when(classificationService.findTreasuryBySemanticKey(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Optional.empty());
      assertThrows(SilFaultException.class, () ->
        service.handlePivotSILChiediAccertamento(userInfo, "token", "org", request)
      );
    }
  }

  @Test
  void givenRichiestaPerIUFWhenHandlePivotSILChiediAccertamentoThenSilFaultException() {
    UserInfo userInfo = Mockito.mock(UserInfo.class);
    String orgIpaCode = "org";
    PivotSILChiediAccertamento request = Mockito.mock(PivotSILChiediAccertamento.class);
    RichiestaPerIUF richiestaPerIUF = Mockito.mock(RichiestaPerIUF.class);
    request.setRichiestaPerIUF(richiestaPerIUF);
    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class);
      MockedStatic<ValidationUtils> validationMock = mockStatic(ValidationUtils.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(isNull(), eq(orgIpaCode))).thenReturn(789L);
      validationMock.when(() -> ValidationUtils.verifyExclusivePresence(isNull(), richiestaPerIUF))
        .thenReturn(true);

      Mockito.when(classificationService.findPaymentsReportingByOrganizationIdAndIuf(Mockito.anyLong(), Mockito.any(), Mockito.any()))
        .thenReturn(Collections.emptyList());
      assertThrows(SilFaultException.class, () ->
        service.handlePivotSILChiediAccertamento(userInfo, "token", "org", request)
      );
    }
  }

  @Test
  void givenRichiestaPerBollettaWhenHandlePivotSILChiediAccertamentoThenSuccess() {
    UserInfo userInfo = Mockito.mock(UserInfo.class);
    String orgIpaCode = "org";
    PivotSILChiediAccertamento request = Mockito.mock(PivotSILChiediAccertamento.class);
    RichiestaPerBolletta richiestaPerBolletta = Mockito.mock(RichiestaPerBolletta.class);
    request.setRichiestaPerBolletta(richiestaPerBolletta);
    Treasury treasury = Mockito.mock(Treasury.class);
    AssessmentsBalanceView assessmentsBalanceView = mock(AssessmentsBalanceView.class);
    PaymentsReporting paymentsReporting = Mockito.mock(PaymentsReporting.class);
    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class);
         MockedStatic<ValidationUtils> validationMock = mockStatic(ValidationUtils.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(isNull(), eq(orgIpaCode))).thenReturn(789L);
      validationMock.when(() -> ValidationUtils.verifyExclusivePresence(richiestaPerBolletta, isNull()))
        .thenReturn(true);

      Mockito.when(classificationService.findTreasuryBySemanticKey(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Optional.of(treasury));
      Mockito.when(treasury.getIuf()).thenReturn("iuf");
      Mockito.when(classificationService.findPaymentsReportingByOrganizationIdAndIuf(Mockito.anyLong(), Mockito.any(), Mockito.any()))
        .thenReturn(List.of(paymentsReporting));
      Mockito.when(debtPositionService.findAuthorizedByTransferSemanticKey(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any()))
        .thenReturn(Optional.of(Mockito.mock(it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentNoPII.class)));
      Mockito.when(classificationService.findClosedAssessmentsBalanceViewByOrganizationIdAndIuds(Mockito.anyLong(), Mockito.anyList(), Mockito.any()))
        .thenReturn(List.of(assessmentsBalanceView));
      Mockito.when(assessmentsBalanceMapper.map2CtBilancio(eq(assessmentsBalanceView))).thenReturn(Mockito.mock(CtBilancio.class));

      PivotSILChiediAccertamentoRisposta resp = service.handlePivotSILChiediAccertamento(userInfo, "token", "org", request);
      assertNotNull(resp);
      assertFalse(resp.getBilancios().isEmpty());
    }
  }
}
