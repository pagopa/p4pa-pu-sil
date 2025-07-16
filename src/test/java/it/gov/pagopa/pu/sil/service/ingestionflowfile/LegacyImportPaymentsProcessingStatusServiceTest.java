package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.dto.generated.DownloadUrl;
import it.gov.pagopa.pu.sil.dto.generated.ImportStatusResponseDTO;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.veneto.regione.pagamenti.ente.PaaSILChiediStatoImportFlusso;
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
class LegacyImportPaymentsProcessingStatusServiceTest {
  @Mock
  IngestionFlowFileService ingestionFlowFileServiceMock;

  private LegacyImportPaymentsProcessingStatusService service;

  @BeforeEach
  void setUp() {
    service = new LegacyImportPaymentsProcessingStatusService(
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
    PaaSILChiediStatoImportFlusso request = new PaaSILChiediStatoImportFlusso();
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFileTypeEnum type = IngestionFlowFileTypeEnum.DP_INSTALLMENTS;

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(false);
      assertThrows(UnauthorizedException.class, () ->
        service.getProcessingStatus(request, userInfo, accessToken, orgIpaCode, 1L, type)
      );
      verifyNoInteractions(ingestionFlowFileServiceMock);
    }
  }

  @Test
  void givenUserAdminWhenGetProcessingStatusThenOk() {
    PaaSILChiediStatoImportFlusso request = new PaaSILChiediStatoImportFlusso();
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

      ImportStatusResponseDTO result = service.getProcessingStatus(request, userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

      assertNotNull(result);
      assertEquals(expectedIngestionFlowFile.getStatus(), result.getStatus());

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenTypeMismatchWhenGetProcessingStatusThenThrowException() {
    PaaSILChiediStatoImportFlusso request = new PaaSILChiediStatoImportFlusso();
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
        service.getProcessingStatus(request, userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.TREASURY_OPI)
      );

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusProcessingWhenGetProcessingStatusThenReturnProcessingStatus() {
    PaaSILChiediStatoImportFlusso request = new PaaSILChiediStatoImportFlusso();
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

      ImportStatusResponseDTO result = service.getProcessingStatus(request, userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

      assertEquals(IngestionFlowFileStatus.PROCESSING, result.getStatus());
      assertNull(result.getDownloadUrls());
      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusCompletedWithoutDownloadFlagsWhenGetProcessingStatusThenReturnCompletedStatusWithoutUrls() {
    PaaSILChiediStatoImportFlusso request = new PaaSILChiediStatoImportFlusso();
    request.setFileScarti(false);
    request.setFileAvvisi(false);
    request.setFileIUV(false);

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

      ImportStatusResponseDTO result = service.getProcessingStatus(request, userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

      assertEquals(IngestionFlowFileStatus.COMPLETED, result.getStatus());
      assertTrue(result.getDownloadUrls() == null || result.getDownloadUrls().isEmpty());
      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusCompletedWithDiscardedFileRequestWhenGetProcessingStatusThenReturnCompletedStatusWithErrorUrl() {
    PaaSILChiediStatoImportFlusso request = new PaaSILChiediStatoImportFlusso();
    request.setFileScarti(true);
    request.setFileAvvisi(false);
    request.setFileIUV(false);

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

      ImportStatusResponseDTO result = service.getProcessingStatus(request, userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

      assertEquals(IngestionFlowFileStatus.COMPLETED, result.getStatus());
      assertNotNull(result.getDownloadUrls());
      assertEquals(1, result.getDownloadUrls().size());

      DownloadUrl downloadUrl = result.getDownloadUrls().get(0);
      assertEquals(DownloadUrl.CodeEnum.DISCARDED_FILE, downloadUrl.getCode());
      assertEquals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/errors", downloadUrl.getUrl());

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusCompletedWithNoticeFileRequestWhenGetProcessingStatusThenReturnCompletedStatusWithNoticeUrl() {
    PaaSILChiediStatoImportFlusso request = new PaaSILChiediStatoImportFlusso();
    request.setFileScarti(false);
    request.setFileAvvisi(true);
    request.setFileIUV(false);

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

      ImportStatusResponseDTO result = service.getProcessingStatus(request, userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

      assertEquals(IngestionFlowFileStatus.COMPLETED, result.getStatus());
      assertNotNull(result.getDownloadUrls());
      assertEquals(1, result.getDownloadUrls().size());

      DownloadUrl downloadUrl = result.getDownloadUrls().get(0);
      assertEquals(DownloadUrl.CodeEnum.PAYMENT_NOTICE_FILE, downloadUrl.getCode());
      assertEquals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/notice", downloadUrl.getUrl());

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusCompletedWithIuvFileRequestWhenGetProcessingStatusThenReturnCompletedStatusWithImportedUrl() {
    PaaSILChiediStatoImportFlusso request = new PaaSILChiediStatoImportFlusso();
    request.setFileScarti(false);
    request.setFileAvvisi(false);
    request.setFileIUV(true);

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

      ImportStatusResponseDTO result = service.getProcessingStatus(request, userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

      assertEquals(IngestionFlowFileStatus.COMPLETED, result.getStatus());
      assertNotNull(result.getDownloadUrls());
      assertEquals(1, result.getDownloadUrls().size());

      DownloadUrl downloadUrl = result.getDownloadUrls().get(0);
      assertEquals(DownloadUrl.CodeEnum.OUTPUT_FILE, downloadUrl.getCode());
      assertEquals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/imported", downloadUrl.getUrl());

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusCompletedWithAllFilesRequestWhenGetProcessingStatusThenReturnCompletedStatusWithAllUrls() {
    PaaSILChiediStatoImportFlusso request = new PaaSILChiediStatoImportFlusso();
    request.setFileScarti(true);
    request.setFileAvvisi(true);
    request.setFileIUV(true);

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

      ImportStatusResponseDTO result = service.getProcessingStatus(request, userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

      assertEquals(IngestionFlowFileStatus.COMPLETED, result.getStatus());
      assertNotNull(result.getDownloadUrls());
      assertEquals(3, result.getDownloadUrls().size());

      // Verify all expected URLs are present
      assertTrue(result.getDownloadUrls().stream().anyMatch(url ->
        url.getCode() == DownloadUrl.CodeEnum.DISCARDED_FILE &&
        url.getUrl().equals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/errors")));

      assertTrue(result.getDownloadUrls().stream().anyMatch(url ->
        url.getCode() == DownloadUrl.CodeEnum.PAYMENT_NOTICE_FILE &&
        url.getUrl().equals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/notice")));

      assertTrue(result.getDownloadUrls().stream().anyMatch(url ->
        url.getCode() == DownloadUrl.CodeEnum.OUTPUT_FILE &&
        url.getUrl().equals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/imported")));

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusErrorWhenGetProcessingStatusThenReturnErrorStatus() {
    PaaSILChiediStatoImportFlusso request = new PaaSILChiediStatoImportFlusso();
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

      ImportStatusResponseDTO result = service.getProcessingStatus(request, userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

      assertEquals(IngestionFlowFileStatus.ERROR, result.getStatus());
      assertNull(result.getDownloadUrls());
      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }
}
