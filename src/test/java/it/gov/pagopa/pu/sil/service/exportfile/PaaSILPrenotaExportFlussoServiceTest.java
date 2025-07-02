package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ExportFileClientException;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILPrenotaExportFlusso;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaaSILPrenotaExportFlussoServiceTest {

  @Mock
  private ExportFileService exportFileServiceMock;
  @Mock
  private DebtPositionService debtPositionServiceMock;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  private PaaSILPrenotaExportFlussoService service;

  @BeforeEach
  void setUp() {
    service = new PaaSILPrenotaExportFlussoService(
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
  void givenUserNotAdminWhenPaaSILPrenotaExportFlussoThenKo() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("userId1");
    String orgIpaCode = "ORG1";
    String accessToken = "token1";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();
    String debtPositionTypeOrgCode = "code";

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(false);

      assertThrows(UnauthorizedException.class, () ->
        service.paaSILPrenotaExportFlusso(
          userInfo,
          accessToken,
          orgIpaCode,
          fileVersion,
          from,
          to,
          debtPositionTypeOrgCode
        )
      );
      verifyNoInteractions(
        exportFileServiceMock,
        debtPositionServiceMock);
    }
  }

  @Test
  void givenUserIsAdminWhenPaaSILPrenotaExportFlussoThenOk() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin2");
    String orgIpaCode = "ORG2";
    Long organizationId = 123L;
    String accessToken = "token2";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();
    String debtPositionTypeOrgCode = "code";

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(organizationId);

      when(exportFileServiceMock.createPaidExportFile(any(), eq(accessToken))).thenReturn(456L);
      when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
        .thenReturn(new DebtPositionTypeOrg().flagActive(true));


      PaaSILPrenotaExportFlusso request = podamFactory.manufacturePojoWithFullData(PaaSILPrenotaExportFlusso.class);
      request.setIdentificativoTipoDovuto(String.valueOf(1L));

      Long result = service.paaSILPrenotaExportFlusso(
        userInfo,
        accessToken,
        orgIpaCode,
        fileVersion,
        from,
        to,
        debtPositionTypeOrgCode
      );

      assertNotNull(result);
      assertEquals(456L, result);
      verify(exportFileServiceMock).createPaidExportFile(any(), eq(accessToken));
    }
  }

  @Test
  void givenNullUserInfoWhenPaaSILPrenotaExportFlussoThenOk() {
    UserInfo userInfo = null;
    String orgIpaCode = "ORG3";
    Long organizationId = 789L;
    String accessToken = "token3";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();
    String debtPositionTypeOrgCode = "code";

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), isNull())).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(isNull(), eq(orgIpaCode))).thenReturn(organizationId);

      when(exportFileServiceMock.createPaidExportFile(any(), eq(accessToken))).thenReturn(999L);
      when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
        .thenReturn(new DebtPositionTypeOrg().flagActive(true));

      PaaSILPrenotaExportFlusso request = podamFactory.manufacturePojoWithFullData(PaaSILPrenotaExportFlusso.class);
      request.setIdentificativoTipoDovuto(String.valueOf(1L));

      Long result = service.paaSILPrenotaExportFlusso(
        userInfo,
        accessToken,
        orgIpaCode,
        fileVersion,
        from,
        to,
        debtPositionTypeOrgCode
      );

      assertNotNull(result);
      assertEquals(999L, result);
      verify(exportFileServiceMock).createPaidExportFile(any(), eq(accessToken));
    }
  }

  @Test
  void givenUserIsAdminAndDebtPositionInvalidWhenPaaSILPrenotaExportFlussoThenException() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin4");
    String orgIpaCode = "ORG4";
    Long organizationId = 123L;
    String accessToken = "token4";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();
    String debtPositionTypeOrgCode = "code";

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(organizationId);

      when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
        .thenReturn(null);

      ExportFileServiceException exception = assertThrows(ExportFileServiceException.class, () ->
        service.paaSILPrenotaExportFlusso(
          userInfo,
          accessToken,
          orgIpaCode,
          fileVersion,
          from,
          to,
          debtPositionTypeOrgCode
        )
      );

      assertEquals(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, exception.getFault());

      verifyNoInteractions(exportFileServiceMock);
    }
  }

  @Test
  void givenUserIsAdminAndInvalidDateIntervalExceptionWhenPaaSILPrenotaExportFlussoThenException() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin5");
    String orgIpaCode = "ORG5";
    Long organizationId = 123L;
    String accessToken = "token5";
    String fileVersion = "1.0";
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = OffsetDateTime.now();
    String debtPositionTypeOrgCode = "code";

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(organizationId);

      ExportFileClientException errorException = new ExportFileClientException(
        ProcessExecutionsErrorDTO.CodeEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE, "");

      when(exportFileServiceMock.createPaidExportFile(any(), eq(accessToken)))
        .thenThrow(errorException);
      when(debtPositionServiceMock.getDebtPositionTypeOrgByOrgIdAndType(eq(organizationId), any(), eq(accessToken)))
        .thenReturn(new DebtPositionTypeOrg().flagActive(true));

      ExportFileClientException exception = assertThrows(ExportFileClientException.class, () ->
        service.paaSILPrenotaExportFlusso(
          userInfo,
          accessToken,
          orgIpaCode,
          fileVersion,
          from,
          to,
          debtPositionTypeOrgCode
        )
      );

      assertEquals(ProcessExecutionsErrorDTO.CodeEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE, exception.getCode());
    }
  }

}
