package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.dto.generated.DownloadUrl;
import it.gov.pagopa.pu.sil.dto.generated.DownloadUrl.CodeEnum;
import it.gov.pagopa.pu.sil.dto.generated.ImportStatusResponseDTO;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionFlowFileProcessingStatusServiceTest {
  @Mock
  private IngestionFlowFileService ingestionFlowFileServiceMock;

  @InjectMocks
  private IngestionFlowFileProcessingStatusService service;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    service = new IngestionFlowFileProcessingStatusService("http://test-url", ingestionFlowFileServiceMock);
  }

  @Test
  void testUnauthorizedAccessThrowsException() {
    String orgIpaCode = "ORG1";
    String accessToken = "accessToken";
    UserInfo userInfo = podamFactory.manufacturePojo(UserInfo.class);
    try (MockedStatic<AuthorizationService> mockedAuth = Mockito.mockStatic(AuthorizationService.class)) {
      mockedAuth.when(() -> AuthorizationService.isAdminRole(orgIpaCode, userInfo)).thenReturn(false);
      assertThrows(UnauthorizedException.class, () ->
        service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      );
    }
  }

  @Test
  void testTypeMismatchThrowsException() {
    Long ingestionFlowFileId = 1L;
    String orgIpaCode = "ORG1";
    String accessToken = "accessToken";
    UserInfo userInfo = podamFactory.manufacturePojo(UserInfo.class);
    IngestionFlowFile file = podamFactory.manufacturePojo(IngestionFlowFile.class)
      .ingestionFlowFileType(IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      .status(IngestionFlowFileStatus.COMPLETED)
      .organizationId(1L)
      .ingestionFlowFileId(1L);
    try (MockedStatic<AuthorizationService> mockedAuth = mockStatic(AuthorizationService.class)) {
      mockedAuth.when(() -> AuthorizationService.isAdminRole(orgIpaCode, userInfo)).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(ingestionFlowFileId, accessToken)).thenReturn(file);
      assertThrows(IllegalArgumentException.class, () ->
        service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.TREASURY_OPI)
      );
    }
  }

  @Test
  void testCompletedStatusWithAllFilesGeneratesDownloadUrls() {
    Long ingestionFlowFileId = 1L;
    String orgIpaCode = "ORG1";
    String accessToken = "accessToken";
    UserInfo userInfo = podamFactory.manufacturePojo(UserInfo.class);
    IngestionFlowFile file = podamFactory.manufacturePojo(IngestionFlowFile.class)
      .ingestionFlowFileType(IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      .status(IngestionFlowFileStatus.COMPLETED)
      .organizationId(1L)
      .ingestionFlowFileId(1L)
      .discardFileName("discarded.csv")
      .pdfGeneratedId("pdfId");
    try (MockedStatic<AuthorizationService> mockedAuth = mockStatic(AuthorizationService.class)) {
      mockedAuth.when(() -> AuthorizationService.isAdminRole(orgIpaCode, userInfo)).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(ingestionFlowFileId, accessToken)).thenReturn(file);
      ImportStatusResponseDTO response = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);
      List<DownloadUrl> urls = response.getDownloadUrls();
      assertEquals(3, urls.size());
      assertTrue(urls.stream().anyMatch(u -> u.getCode() == CodeEnum.OUTPUT_FILE));
      assertTrue(urls.stream().anyMatch(u -> u.getCode() == CodeEnum.DISCARDED_FILE));
      assertTrue(urls.stream().anyMatch(u -> u.getCode() == CodeEnum.PAYMENT_NOTICE_FILE));
    }
  }

  @Test
  void testCompletedStatusWithOnlyOutputFileGeneratesOneUrl() {
    Long ingestionFlowFileId = 1L;
    String orgIpaCode = "ORG1";
    String accessToken = "accessToken";
    UserInfo userInfo = podamFactory.manufacturePojo(UserInfo.class);
    IngestionFlowFile file = podamFactory.manufacturePojo(IngestionFlowFile.class)
      .ingestionFlowFileType(IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      .status(IngestionFlowFileStatus.COMPLETED)
      .organizationId(1L)
      .ingestionFlowFileId(1L)
      .discardFileName(null)
      .pdfGeneratedId(null);
    try (MockedStatic<AuthorizationService> mockedAuth = mockStatic(AuthorizationService.class)) {
      mockedAuth.when(() -> AuthorizationService.isAdminRole(orgIpaCode, userInfo)).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(ingestionFlowFileId, accessToken)).thenReturn(file);
      ImportStatusResponseDTO response = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);
      List<DownloadUrl> urls = response.getDownloadUrls();
      assertEquals(1, urls.size());
      assertEquals(CodeEnum.OUTPUT_FILE, urls.get(0).getCode());
    }
  }

  @Test
  void testNonCompletedStatusReturnsNoDownloadUrls() {
    Long ingestionFlowFileId = 1L;
    String orgIpaCode = "ORG1";
    String accessToken = "accessToken";
    UserInfo userInfo = podamFactory.manufacturePojo(UserInfo.class);
    IngestionFlowFile file = podamFactory.manufacturePojo(IngestionFlowFile.class)
      .ingestionFlowFileType(IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      .status(IngestionFlowFileStatus.PROCESSING)
      .organizationId(1L)
      .ingestionFlowFileId(1L);
    try (MockedStatic<AuthorizationService> mockedAuth = mockStatic(AuthorizationService.class)) {
      mockedAuth.when(() -> AuthorizationService.isAdminRole(orgIpaCode, userInfo)).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(ingestionFlowFileId, accessToken)).thenReturn(file);
      ImportStatusResponseDTO response = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);
      assertTrue(response.getDownloadUrls() == null || response.getDownloadUrls().isEmpty());
    }
  }

  @Test
  void testNullOrEmptyExpectedTypesDoesNotThrow() {
    Long ingestionFlowFileId = 1L;
    String orgIpaCode = "ORG1";
    String accessToken = "accessToken";
    UserInfo userInfo = podamFactory.manufacturePojo(UserInfo.class);
    IngestionFlowFile file = podamFactory.manufacturePojo(IngestionFlowFile.class)
      .status(IngestionFlowFileStatus.COMPLETED)
      .ingestionFlowFileType(IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      .organizationId(1L)
      .ingestionFlowFileId(1L);
    try (MockedStatic<AuthorizationService> mockedAuth = mockStatic(AuthorizationService.class)) {
      mockedAuth.when(() -> AuthorizationService.isAdminRole(orgIpaCode, userInfo)).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(ingestionFlowFileId, accessToken)).thenReturn(file);
      assertDoesNotThrow(() -> service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L));
      assertDoesNotThrow(() -> service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, (IngestionFlowFileTypeEnum[]) null));
      assertDoesNotThrow(() -> service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L));
    }
  }
}
