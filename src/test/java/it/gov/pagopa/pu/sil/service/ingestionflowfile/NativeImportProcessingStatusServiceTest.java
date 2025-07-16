package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.dto.generated.DownloadUrl;
import it.gov.pagopa.pu.sil.dto.generated.ImportStatusResponseDTO;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
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
class NativeImportProcessingStatusServiceTest {
  @Mock
  IngestionFlowFileService ingestionFlowFileServiceMock;

  private NativeImportProcessingStatusService service;

  @BeforeEach
  void setUp() {
    service = new NativeImportProcessingStatusService(
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

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(false);
      assertThrows(UnauthorizedException.class, () ->
        service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L)
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
      .status(IngestionFlowFileStatus.PROCESSING);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);

      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken))
        .thenReturn(expectedIngestionFlowFile);

      ImportStatusResponseDTO result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L);

      assertNotNull(result);
      assertEquals(expectedIngestionFlowFile.getStatus(), result.getStatus());

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
      .status(IngestionFlowFileStatus.PROCESSING)
      .organizationId(1L)
      .ingestionFlowFileId(1L);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken)).thenReturn(ingestionFlowFile);

      ImportStatusResponseDTO result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L);

      assertEquals(IngestionFlowFileStatus.PROCESSING, result.getStatus());
      assertNull(result.getDownloadUrls());
      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusErrorWhenGetProcessingStatusThenReturnErrorStatus() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFile ingestionFlowFile = new IngestionFlowFile()
      .status(IngestionFlowFileStatus.ERROR)
      .organizationId(1L)
      .ingestionFlowFileId(1L);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken)).thenReturn(ingestionFlowFile);

      ImportStatusResponseDTO result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L);

      assertEquals(IngestionFlowFileStatus.ERROR, result.getStatus());
      assertNull(result.getDownloadUrls());
      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusCompletedWithoutOptionalFilesWhenGetProcessingStatusThenReturnCompletedStatusWithOutputUrlOnly() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFile ingestionFlowFile = new IngestionFlowFile()
      .status(IngestionFlowFileStatus.COMPLETED)
      .organizationId(1L)
      .ingestionFlowFileId(1L)
      .discardFileName(null)
      .pdfGeneratedId(null);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken)).thenReturn(ingestionFlowFile);

      ImportStatusResponseDTO result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L);

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
  void givenStatusCompletedWithDiscardFileWhenGetProcessingStatusThenReturnCompletedStatusWithOutputAndDiscardUrls() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFile ingestionFlowFile = new IngestionFlowFile()
      .status(IngestionFlowFileStatus.COMPLETED)
      .organizationId(1L)
      .ingestionFlowFileId(1L)
      .discardFileName("discarded_records.csv")
      .pdfGeneratedId(null);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken)).thenReturn(ingestionFlowFile);

      ImportStatusResponseDTO result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L);

      assertEquals(IngestionFlowFileStatus.COMPLETED, result.getStatus());
      assertNotNull(result.getDownloadUrls());
      assertEquals(2, result.getDownloadUrls().size());

      // Verify OUTPUT_FILE URL is present
      assertTrue(result.getDownloadUrls().stream().anyMatch(url ->
        url.getCode() == DownloadUrl.CodeEnum.OUTPUT_FILE &&
          url.getUrl().equals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/imported")));

      // Verify DISCARDED_FILE URL is present
      assertTrue(result.getDownloadUrls().stream().anyMatch(url ->
        url.getCode() == DownloadUrl.CodeEnum.DISCARDED_FILE &&
          url.getUrl().equals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/errors")));

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusCompletedWithPdfGeneratedWhenGetProcessingStatusThenReturnCompletedStatusWithOutputAndNoticeUrls() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFile ingestionFlowFile = new IngestionFlowFile()
      .status(IngestionFlowFileStatus.COMPLETED)
      .organizationId(1L)
      .ingestionFlowFileId(1L)
      .discardFileName(null)
      .pdfGeneratedId("pdf-123");

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken)).thenReturn(ingestionFlowFile);

      ImportStatusResponseDTO result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L);

      assertEquals(IngestionFlowFileStatus.COMPLETED, result.getStatus());
      assertNotNull(result.getDownloadUrls());
      assertEquals(2, result.getDownloadUrls().size());

      // Verify OUTPUT_FILE URL is present
      assertTrue(result.getDownloadUrls().stream().anyMatch(url ->
        url.getCode() == DownloadUrl.CodeEnum.OUTPUT_FILE &&
          url.getUrl().equals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/imported")));

      // Verify PAYMENT_NOTICE_FILE URL is present
      assertTrue(result.getDownloadUrls().stream().anyMatch(url ->
        url.getCode() == DownloadUrl.CodeEnum.PAYMENT_NOTICE_FILE &&
          url.getUrl().equals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/notice")));

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusCompletedWithAllOptionalFilesWhenGetProcessingStatusThenReturnCompletedStatusWithAllUrls() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFile ingestionFlowFile = new IngestionFlowFile()
      .status(IngestionFlowFileStatus.COMPLETED)
      .organizationId(1L)
      .ingestionFlowFileId(1L)
      .discardFileName("discarded_records.csv")
      .pdfGeneratedId("pdf-123");

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken)).thenReturn(ingestionFlowFile);

      ImportStatusResponseDTO result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L);

      assertEquals(IngestionFlowFileStatus.COMPLETED, result.getStatus());
      assertNotNull(result.getDownloadUrls());
      assertEquals(3, result.getDownloadUrls().size());

      // Verify OUTPUT_FILE URL is present
      assertTrue(result.getDownloadUrls().stream().anyMatch(url ->
        url.getCode() == DownloadUrl.CodeEnum.OUTPUT_FILE &&
          url.getUrl().equals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/imported")));

      // Verify DISCARDED_FILE URL is present
      assertTrue(result.getDownloadUrls().stream().anyMatch(url ->
        url.getCode() == DownloadUrl.CodeEnum.DISCARDED_FILE &&
          url.getUrl().equals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/errors")));

      // Verify PAYMENT_NOTICE_FILE URL is present
      assertTrue(result.getDownloadUrls().stream().anyMatch(url ->
        url.getCode() == DownloadUrl.CodeEnum.PAYMENT_NOTICE_FILE &&
          url.getUrl().equals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/notice")));

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusCompletedWithEmptyDiscardFileNameWhenGetProcessingStatusThenTreatAsNoDiscardFile() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFile ingestionFlowFile = new IngestionFlowFile()
      .status(IngestionFlowFileStatus.COMPLETED)
      .organizationId(1L)
      .ingestionFlowFileId(1L)
      .discardFileName("")
      .pdfGeneratedId(null);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken)).thenReturn(ingestionFlowFile);

      ImportStatusResponseDTO result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L);

      assertEquals(IngestionFlowFileStatus.COMPLETED, result.getStatus());
      assertNotNull(result.getDownloadUrls());
      assertEquals(1, result.getDownloadUrls().size());

      DownloadUrl downloadUrl = result.getDownloadUrls().get(0);
      assertEquals(DownloadUrl.CodeEnum.OUTPUT_FILE, downloadUrl.getCode());

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusCompletedWithEmptyPdfGeneratedIdWhenGetProcessingStatusThenTreatAsNoPdfFile() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFile ingestionFlowFile = new IngestionFlowFile()
      .status(IngestionFlowFileStatus.COMPLETED)
      .organizationId(1L)
      .ingestionFlowFileId(1L)
      .discardFileName(null)
      .pdfGeneratedId("");

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken)).thenReturn(ingestionFlowFile);

      ImportStatusResponseDTO result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L);

      assertEquals(IngestionFlowFileStatus.COMPLETED, result.getStatus());
      assertNotNull(result.getDownloadUrls());
      assertEquals(1, result.getDownloadUrls().size());

      DownloadUrl downloadUrl = result.getDownloadUrls().get(0);
      assertEquals(DownloadUrl.CodeEnum.OUTPUT_FILE, downloadUrl.getCode());

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }
}
