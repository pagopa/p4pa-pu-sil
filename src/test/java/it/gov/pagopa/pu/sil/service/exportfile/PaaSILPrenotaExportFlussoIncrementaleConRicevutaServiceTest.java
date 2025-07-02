package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ExportFileClientException;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;

@ExtendWith(MockitoExtension.class)
class PaaSILPrenotaExportFlussoIncrementaleConRicevutaServiceTest {

  @Mock
  private ExportFileService exportFileServiceMock;
  @Mock
  private DebtPositionService debtPositionServiceMock;

  private PaaSILPrenotaExportFlussoIncrementaleConRicevutaService service;

  @BeforeEach
  void setUp() {
    service = new PaaSILPrenotaExportFlussoIncrementaleConRicevutaService(
      exportFileServiceMock,
      debtPositionServiceMock
    );
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(
      exportFileServiceMock,
      debtPositionServiceMock
    );
  }

  @Test
  void givenUserNotAdminWhenDoReservationThenKo() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("userId1");
    String orgIpaCode = "ORG1";
    String accessToken = "token1";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();
    String debtPositionTypeOrgCode = "code";
    boolean incremental = true;

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(false);
      assertThrows(UnauthorizedException.class, () ->
        service.doReservation(
          userInfo,
          accessToken,
          orgIpaCode,
          fileVersion,
          from,
          to,
          debtPositionTypeOrgCode,
          incremental
        )
      );
      verifyNoInteractions(exportFileServiceMock, debtPositionServiceMock);
    }
  }

  @Test
  void givenUserIsAdminWhenDoReservationThenOk() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin2");
    String orgIpaCode = "ORG2";
    Long organizationId = 123L;
    String accessToken = "token2";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();
    String debtPositionTypeOrgCode = "code";
    boolean incremental = true;

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(organizationId);
      DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg().flagActive(true).debtPositionTypeOrgId(1L);
      when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
        .thenReturn(debtPositionTypeOrg);
      when(exportFileServiceMock.createPaidExportFile(any(), eq(accessToken))).thenReturn(456L);

      Long result = service.doReservation(
        userInfo,
        accessToken,
        orgIpaCode,
        fileVersion,
        from,
        to,
        debtPositionTypeOrgCode,
        incremental
      );
      assertNotNull(result);
      assertEquals(456L, result);
      verify(exportFileServiceMock).createPaidExportFile(any(), eq(accessToken));
    }
  }

  @Test
  void givenNullUserInfoWhenDoReservationThenOk() {
    UserInfo userInfo = null;
    String orgIpaCode = "ORG3";
    Long organizationId = 789L;
    String accessToken = "token3";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();
    String debtPositionTypeOrgCode = "code";
    boolean incremental = false;

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), isNull())).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(isNull(), eq(orgIpaCode))).thenReturn(organizationId);
      DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg().flagActive(true).debtPositionTypeOrgId(2L);
      when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
        .thenReturn(debtPositionTypeOrg);
      when(exportFileServiceMock.createPaidExportFile(any(), eq(accessToken))).thenReturn(999L);

      Long result = service.doReservation(
        userInfo,
        accessToken,
        orgIpaCode,
        fileVersion,
        from,
        to,
        debtPositionTypeOrgCode,
        incremental
      );
      assertNotNull(result);
      assertEquals(999L, result);
      verify(exportFileServiceMock).createPaidExportFile(any(), eq(accessToken));
    }
  }

  @Test
  void givenUserIsAdminAndDebtPositionInvalidWhenDoReservationThenException() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin4");
    String orgIpaCode = "ORG4";
    Long organizationId = 123L;
    String accessToken = "token4";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();
    String debtPositionTypeOrgCode = "code";
    boolean incremental = true;

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(organizationId);
      when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
        .thenReturn(null);

      ExportFileServiceException exception = assertThrows(ExportFileServiceException.class, () ->
        service.doReservation(
          userInfo,
          accessToken,
          orgIpaCode,
          fileVersion,
          from,
          to,
          debtPositionTypeOrgCode,
          incremental
        )
      );
      assertEquals(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, exception.getFault());
      verifyNoInteractions(exportFileServiceMock);
    }
  }

  @Test
  void givenUserIsAdminAndInvalidDateIntervalExceptionWhenDoreervationThenException() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin5");
    String orgIpaCode = "ORG5";
    Long organizationId = 123L;
    String accessToken = "token5";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();
    String debtPositionTypeOrgCode = "code";
    boolean incremental = false;

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(organizationId);
      DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg().flagActive(true).debtPositionTypeOrgId(3L);
      when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
        .thenReturn(debtPositionTypeOrg);
      ExportFileClientException errorException = new ExportFileClientException(
        ProcessExecutionsErrorDTO.CodeEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE, "");
      when(exportFileServiceMock.createPaidExportFile(any(), eq(accessToken)))
        .thenThrow(errorException);

      ExportFileClientException exception = assertThrows(ExportFileClientException.class, () ->
        service.doReservation(
          userInfo,
          accessToken,
          orgIpaCode,
          fileVersion,
          from,
          to,
          debtPositionTypeOrgCode,
          incremental
        )
      );
      assertEquals(ProcessExecutionsErrorDTO.CodeEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE, exception.getCode());
    }
  }
}
