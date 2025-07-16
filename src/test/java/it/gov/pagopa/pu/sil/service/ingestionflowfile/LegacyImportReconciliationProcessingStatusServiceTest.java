package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.dto.generated.ImportStatusResponseDTO;
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
class LegacyImportReconciliationProcessingStatusServiceTest {
  @Mock
  IngestionFlowFileService ingestionFlowFileServiceMock;

  private LegacyImportReconciliationProcessingStatusService service;

  @BeforeEach
  void setUp() {
    service = new LegacyImportReconciliationProcessingStatusService(
      "http://fileshare.example.com",
      ingestionFlowFileServiceMock
    );
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(ingestionFlowFileServiceMock);
  }

  @Test
  void givenUserNotAdminWhenGetProcessingStatusThenKo() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFileTypeEnum type = IngestionFlowFileTypeEnum.DP_INSTALLMENTS;

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(false);
      assertThrows(UnauthorizedException.class, () ->
        service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, type)
      );
      verifyNoInteractions(ingestionFlowFileServiceMock);
    }
  }

  @Test
  void givenUserAdminWhenGetProcessingStatusThenOk() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFile expectedIngestionFlowFile = new IngestionFlowFile()
      .ingestionFlowFileType(IngestionFlowFile.IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      .status(IngestionFlowFileStatus.PROCESSING);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);

      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken))
        .thenReturn(expectedIngestionFlowFile);

      ImportStatusResponseDTO result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

      assertNotNull(result);
      assertEquals(expectedIngestionFlowFile.getStatus(), result.getStatus());

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenTypeMismatchWhenGetProcessingStatusThenThrowException() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFile expectedIngestionFlowFile = new IngestionFlowFile()
      .ingestionFlowFileType(IngestionFlowFile.IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken))
        .thenReturn(expectedIngestionFlowFile);

      assertThrows(IllegalArgumentException.class, () ->
        service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.TREASURY_OPI)
      );

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusProcessingWhenGetProcessingStatusThenReturnProcessingStatus() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFile ingestionFlowFile = new IngestionFlowFile()
      .ingestionFlowFileType(IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      .status(IngestionFlowFileStatus.PROCESSING)
      .organizationId(1L)
      .ingestionFlowFileId(1L);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken)).thenReturn(ingestionFlowFile);

      ImportStatusResponseDTO result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

      assertEquals(IngestionFlowFileStatus.PROCESSING, result.getStatus());
      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusCompletedWhenGetProcessingStatusThenReturnCompletedStatus() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFile ingestionFlowFile = new IngestionFlowFile()
      .ingestionFlowFileType(IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      .status(IngestionFlowFileStatus.COMPLETED)
      .organizationId(1L)
      .ingestionFlowFileId(1L);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken)).thenReturn(ingestionFlowFile);

      ImportStatusResponseDTO result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

      assertEquals(IngestionFlowFileStatus.COMPLETED, result.getStatus());
      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusFailedWhenGetProcessingStatusThenReturnFailedStatus() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFile ingestionFlowFile = new IngestionFlowFile()
      .ingestionFlowFileType(IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      .status(IngestionFlowFileStatus.ERROR)
      .organizationId(1L)
      .ingestionFlowFileId(1L);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken)).thenReturn(ingestionFlowFile);

      ImportStatusResponseDTO result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

      assertEquals(IngestionFlowFileStatus.ERROR, result.getStatus());
      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }
}
