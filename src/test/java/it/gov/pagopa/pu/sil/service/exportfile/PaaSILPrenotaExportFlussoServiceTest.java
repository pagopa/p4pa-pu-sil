package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.veneto.regione.pagamenti.ente.PaaSILPrenotaExportFlusso;
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

@ExtendWith(MockitoExtension.class)
class PaaSILPrenotaExportFlussoServiceTest {

  @Mock
  private ExportFileService exportFileServiceMock;

  private PaaSILPrenotaExportFlussoService service;

  @BeforeEach
  void setUp() {
    service = new PaaSILPrenotaExportFlussoService(
      exportFileServiceMock
    );
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(
      exportFileServiceMock
    );
  }

  @Test
  void givenUserNotAdminWhenPaaSILPrenotaExportFlussoThenKo() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("userId1");
    String orgIpaCode = "ORG1";
    String accessToken = "token1";

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(false);

      assertThrows(UnauthorizedException.class, () ->
        service.paaSILPrenotaExportFlusso(
          userInfo,
          accessToken,
          orgIpaCode,
          new PaaSILPrenotaExportFlusso()
        )
      );
      verifyNoInteractions(exportFileServiceMock);
    }
  }

  @Test
  void givenUserIsAdminWhenPaaSILPrenotaExportFlussoThenOk() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin2");
    String orgIpaCode = "ORG2";
    String accessToken = "token2";

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(123L);

      when(exportFileServiceMock.createPaidExportFile(any(), eq(accessToken))).thenReturn(456L);

      Long result = service.paaSILPrenotaExportFlusso(
        userInfo,
        accessToken,
        orgIpaCode,
        new PaaSILPrenotaExportFlusso()
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
    String accessToken = "token3";

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), isNull())).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(isNull(), eq(orgIpaCode))).thenReturn(789L);

      when(exportFileServiceMock.createPaidExportFile(any(), eq(accessToken))).thenReturn(999L);

      Long result = service.paaSILPrenotaExportFlusso(
        userInfo,
        accessToken,
        orgIpaCode,
        new PaaSILPrenotaExportFlusso()
      );

      assertNotNull(result);
      assertEquals(999L, result);
      verify(exportFileServiceMock).createPaidExportFile(any(), eq(accessToken));
    }
  }
}
