package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.dto.PaymentsProcessingStatusDTO;
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
class IngestionFlowFileProcessingStatusServiceTest {
  @Mock
  IngestionFlowFileService ingestionFlowFileServiceMock;

  private IngestionFlowFileProcessingStatusService service;

  @BeforeEach
  void setUp() {
    service = new IngestionFlowFileProcessingStatusService(ingestionFlowFileServiceMock,
      "http://fileshare.example.com");
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(ingestionFlowFileServiceMock);
  }

  @Test
  void givenUserNotAdminWhenGetIngestionFlowFileThenKo() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFileTypeEnum type = IngestionFlowFileTypeEnum.DP_INSTALLMENTS;

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(false);
      assertThrows(UnauthorizedException.class, () ->
        service.getIngestionFlowFile(userInfo, accessToken, orgIpaCode, 1L, type)
      );
      verifyNoInteractions(ingestionFlowFileServiceMock);
    }
  }

  @Test
  void givenUserAdminWhenGetIngestionFlowFileThenOk() {
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

      IngestionFlowFile result = service.getIngestionFlowFile(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

      assertNotNull(result);
      assertEquals(expectedIngestionFlowFile.getStatus(), result.getStatus());

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenTypeMismatchWhenGetIngestionFlowFileThenThrowException() {
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
        service.getIngestionFlowFile(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.TREASURY_OPI)
      );

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusNotCompletedWhenGetProcessingStatusThenNoUrls() {
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
      PaaSILChiediStatoImportFlusso req = new PaaSILChiediStatoImportFlusso();
      req.setFileScarti(true);
      req.setFileAvvisi(true);
      req.setFileIUV(true);
      PaymentsProcessingStatusDTO result = service.getProcessingStatus(req, userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);
      assertNull(result.getUrlErrors());
      assertNull(result.getUrlNotice());
      assertNull(result.getUrlImported());
      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusCompletedAndAllFlagsTrueWhenGetProcessingStatusThenAllUrlsSet() {
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
      PaaSILChiediStatoImportFlusso req = new PaaSILChiediStatoImportFlusso();
      req.setFileScarti(true);
      req.setFileAvvisi(true);
      req.setFileIUV(true);
      PaymentsProcessingStatusDTO result = service.getProcessingStatus(req, userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);
      assertEquals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/errors", result.getUrlErrors());
      assertEquals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/notice", result.getUrlNotice());
      assertEquals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/imported", result.getUrlImported());
      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusCompletedAndAllFlagsFalseWhenGetProcessingStatusThenNoUrlsSet() {
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
      PaaSILChiediStatoImportFlusso req = new PaaSILChiediStatoImportFlusso();
      req.setFileScarti(false);
      req.setFileAvvisi(false);
      req.setFileIUV(false);
      PaymentsProcessingStatusDTO result = service.getProcessingStatus(req, userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);
      assertNull(result.getUrlErrors());
      assertNull(result.getUrlNotice());
      assertNull(result.getUrlImported());
      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenStatusCompletedAndOnlySomeFlagsTrueWhenGetProcessingStatusThenOnlySomeUrlsSet() {
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
      PaaSILChiediStatoImportFlusso req = new PaaSILChiediStatoImportFlusso();
      req.setFileScarti(true);
      req.setFileAvvisi(false);
      req.setFileIUV(true);
      PaymentsProcessingStatusDTO result = service.getProcessingStatus(req, userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);
      assertEquals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/errors", result.getUrlErrors());
      assertNull(result.getUrlNotice());
      assertEquals("http://fileshare.example.com/organization/1/ingestionflowfiles/1/imported", result.getUrlImported());
      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }


}
