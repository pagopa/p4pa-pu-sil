package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.dto.generated.DownloadUrl;
import it.gov.pagopa.pu.sil.dto.generated.DownloadUrl.CodeEnum;
import it.gov.pagopa.pu.sil.dto.generated.ImportStatusResponseDTO;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      ingestionFlowFileServiceMock
    );
  }

  @Test
  void testUnauthorizedAccessThrowsException() {
    // Given
    String orgIpaCode = "ORG1";
    String accessToken = "accessToken";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", "OTHERIPACODE");

    // When, Then
    assertThrows(AuthorizationDeniedException.class, () ->
      service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
    );
  }

  @Test
  void testTypeMismatchThrowsException() {
    // Given
    Long ingestionFlowFileId = 1L;
    String orgIpaCode = "ORG1";
    String accessToken = "accessToken";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);

    IngestionFlowFile file = podamFactory.manufacturePojo(IngestionFlowFile.class)
      .ingestionFlowFileType(IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      .status(IngestionFlowFileStatus.COMPLETED)
      .organizationId(1L)
      .ingestionFlowFileId(1L);

    when(ingestionFlowFileServiceMock.getIngestionFlowFile(ingestionFlowFileId, accessToken))
      .thenReturn(file);

    // When, Then
    assertThrows(IllegalArgumentException.class, () ->
      service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.TREASURY_OPI)
    );
  }

  @Test
  void testCompletedStatusWithAllFilesGeneratesDownloadUrls() {
    // Given
    Long ingestionFlowFileId = 1L;
    String orgIpaCode = "ORG1";
    String accessToken = "accessToken";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    IngestionFlowFile file = podamFactory.manufacturePojo(IngestionFlowFile.class)
      .ingestionFlowFileType(IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      .status(IngestionFlowFileStatus.COMPLETED)
      .organizationId(1L)
      .ingestionFlowFileId(1L)
      .discardFileName("discarded.csv")
      .pdfGeneratedId("pdfId");

    when(ingestionFlowFileServiceMock.getIngestionFlowFile(ingestionFlowFileId, accessToken))
      .thenReturn(file);

    // When
    ImportStatusResponseDTO response = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

    // Then
    List<DownloadUrl> urls = response.getDownloadUrls();
    assertEquals(3, urls.size());
    assertTrue(urls.stream().anyMatch(u -> u.getCode() == CodeEnum.OUTPUT_FILE));
    assertTrue(urls.stream().anyMatch(u -> u.getCode() == CodeEnum.DISCARDED_FILE));
    assertTrue(urls.stream().anyMatch(u -> u.getCode() == CodeEnum.PAYMENT_NOTICE_FILE));
  }

  @Test
  void testCompletedStatusWithOnlyOutputFileGeneratesOneUrl() {
    // Given
    Long ingestionFlowFileId = 1L;
    String orgIpaCode = "ORG1";
    String accessToken = "accessToken";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    IngestionFlowFile file = podamFactory.manufacturePojo(IngestionFlowFile.class)
      .ingestionFlowFileType(IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      .status(IngestionFlowFileStatus.COMPLETED)
      .organizationId(1L)
      .ingestionFlowFileId(1L)
      .discardFileName(null)
      .pdfGeneratedId(null);
    when(ingestionFlowFileServiceMock.getIngestionFlowFile(ingestionFlowFileId, accessToken))
      .thenReturn(file);

    // When
    ImportStatusResponseDTO response = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

    //Then
    List<DownloadUrl> urls = response.getDownloadUrls();
    assertEquals(1, urls.size());
    assertEquals(CodeEnum.OUTPUT_FILE, urls.getFirst().getCode());
  }

  @Test
  void testNonCompletedStatusReturnsNoDownloadUrls() {
    // Given
    Long ingestionFlowFileId = 1L;
    String orgIpaCode = "ORG1";
    String accessToken = "accessToken";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    IngestionFlowFile file = podamFactory.manufacturePojo(IngestionFlowFile.class)
      .ingestionFlowFileType(IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      .status(IngestionFlowFileStatus.PROCESSING)
      .organizationId(1L)
      .ingestionFlowFileId(1L);

    when(ingestionFlowFileServiceMock.getIngestionFlowFile(ingestionFlowFileId, accessToken))
      .thenReturn(file);

    //When
    ImportStatusResponseDTO response = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

    // Then
    assertTrue(response.getDownloadUrls() == null || response.getDownloadUrls().isEmpty());
  }

  @Test
  void testNullOrEmptyExpectedTypesDoesNotThrow() {
    // Given
    Long ingestionFlowFileId = 1L;
    String orgIpaCode = "ORG1";
    String accessToken = "accessToken";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    IngestionFlowFile file = podamFactory.manufacturePojo(IngestionFlowFile.class)
      .status(IngestionFlowFileStatus.COMPLETED)
      .ingestionFlowFileType(IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      .organizationId(1L)
      .ingestionFlowFileId(1L);

    when(ingestionFlowFileServiceMock.getIngestionFlowFile(ingestionFlowFileId, accessToken))
      .thenReturn(file);

    // When, Then
    assertDoesNotThrow(() -> service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L));
    assertDoesNotThrow(() -> service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, (IngestionFlowFileTypeEnum[]) null));
    assertDoesNotThrow(() -> service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L));
  }
}
