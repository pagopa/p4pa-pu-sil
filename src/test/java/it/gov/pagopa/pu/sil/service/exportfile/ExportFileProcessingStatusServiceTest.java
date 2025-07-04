package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile.ExportFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFileStatus;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportFileProcessingStatusServiceTest {
  @Mock
  ExportFileService exportFileServiceMock;

  private ExportFileProcessingStatusService service;

  @BeforeEach
  void setUp() {
    service = new ExportFileProcessingStatusService(exportFileServiceMock, "http://fileshare.example.com");
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(exportFileServiceMock);
  }

  @Test
  void givenUserNotAdminWhenGetProcessingStatusThenUnauthorized() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    ExportFileTypeEnum type = ExportFileTypeEnum.PAID;
    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(false);
      assertThrows(UnauthorizedException.class, () ->
        service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, type)
      );
      verifyNoInteractions(exportFileServiceMock);
    }
  }

  @Test
  void givenUserAdminWhenTypeMismatchThenThrowException() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    ExportFile exportFile = new ExportFile()
      .exportFileType(ExportFileTypeEnum.CLASSIFICATIONS)
      .status(ExportFileStatus.PROCESSING);
    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(exportFileServiceMock.getExportFile(1L, accessToken)).thenReturn(exportFile);
      assertThrows(IllegalArgumentException.class, () ->
        service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, ExportFileTypeEnum.PAID)
      );
      verify(exportFileServiceMock).getExportFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusNotCompletedWhenGetProcessingStatusThenNoUrl() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    ExportFile exportFile = new ExportFile()
      .exportFileType(ExportFileTypeEnum.PAID)
      .status(ExportFileStatus.PROCESSING)
      .organizationId(123L)
      .exportFileId(1L);
    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(exportFileServiceMock.getExportFile(1L, accessToken)).thenReturn(exportFile);
      Pair<ExportFileStatus, String> result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, ExportFileTypeEnum.PAID);
      assertEquals(ExportFileStatus.PROCESSING, result.getLeft());
      assertNull(result.getRight());
      verify(exportFileServiceMock).getExportFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusCompletedWhenGetProcessingStatusThenUrlReturned() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    ExportFile exportFile = new ExportFile()
      .exportFileType(ExportFileTypeEnum.PAID)
      .status(ExportFileStatus.COMPLETED)
      .organizationId(123L)
      .exportFileId(123456L);
    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(exportFileServiceMock.getExportFile(123456L, accessToken)).thenReturn(exportFile);
      Pair<ExportFileStatus, String> result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 123456L, ExportFileTypeEnum.PAID);
      assertEquals(ExportFileStatus.COMPLETED, result.getLeft());
      assertNotNull(result.getRight());
      assertTrue(result.getRight().contains("123456"));
      assertTrue(result.getRight().contains("123"));
      verify(exportFileServiceMock).getExportFile(123456L, accessToken);
    }
  }
}
