package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile.ExportFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFileStatus;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.dto.generated.ExportStatusResponseDTO;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
    // Given
    String orgIpaCode = "ORG1";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", "OTHERIPACODE");
    userInfo.setUserId("user1");
    String accessToken = "token";
    ExportFileTypeEnum type = ExportFileTypeEnum.PAID;

    // When, Then
    assertThrows(AuthorizationDeniedException.class, () ->
      service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, type)
    );
  }

  @Test
  void givenUserAdminWhenTypeMismatchThenThrowException() {
    // Given
    String orgIpaCode = "ORG1";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    userInfo.setUserId("user1");
    String accessToken = "token";
    ExportFile exportFile = new ExportFile()
      .exportFileType(ExportFileTypeEnum.CLASSIFICATIONS)
      .status(ExportFileStatus.PROCESSING);

    when(exportFileServiceMock.getExportFile(1L, accessToken)).thenReturn(exportFile);

    // When, Then
    assertThrows(IllegalArgumentException.class, () ->
      service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, ExportFileTypeEnum.PAID)
    );
  }

  @Test
  void givenNotExistingExportWhenGetProcessingStatusThenThrowException() {
    // Given
    String orgIpaCode = "ORG1";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    userInfo.setUserId("user1");
    String accessToken = "token";

    when(exportFileServiceMock.getExportFile(1L, accessToken)).thenReturn(null);

    // When, Then
    assertThrows(IllegalArgumentException.class, () ->
      service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, ExportFileTypeEnum.PAID)
    );
  }

  @Test
  void givenStatusNotCompletedWhenGetProcessingStatusThenNoUrl() {
    // Given
    String orgIpaCode = "ORG1";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    userInfo.setUserId("user1");
    String accessToken = "token";
    ExportFile exportFile = new ExportFile()
      .exportFileType(ExportFileTypeEnum.PAID)
      .status(ExportFileStatus.PROCESSING)
      .organizationId(123L)
      .exportFileId(1L);

    when(exportFileServiceMock.getExportFile(1L, accessToken)).thenReturn(exportFile);

    // When
    Pair<ExportStatusResponseDTO.StatusEnum, String> result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, ExportFileTypeEnum.PAID);

    // Then
    assertEquals(ExportStatusResponseDTO.StatusEnum.PROCESSING, result.getLeft());
    assertNull(result.getRight());
  }

  @ParameterizedTest
  @ValueSource(longs = {0L})
  @NullSource
  void givenStatusCompletedButNoRowsWhenGetProcessingStatusThenNoUrl(Long numTotalRows) {
    // Given
    String orgIpaCode = "ORG1";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    userInfo.setUserId("user1");
    String accessToken = "token";
    ExportFile exportFile = new ExportFile()
      .exportFileType(ExportFileTypeEnum.PAID)
      .status(ExportFileStatus.COMPLETED)
      .numTotalRows(numTotalRows)
      .organizationId(123L)
      .exportFileId(1L);

    when(exportFileServiceMock.getExportFile(1L, accessToken)).thenReturn(exportFile);

    // When
    Pair<ExportStatusResponseDTO.StatusEnum, String> result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, ExportFileTypeEnum.PAID);

    // Then
    assertEquals(ExportStatusResponseDTO.StatusEnum.COMPLETED_NO_DATA_FOUND, result.getLeft());
    assertNull(result.getRight());
  }

  @Test
  void givenStatusCompletedWhenGetProcessingStatusThenUrlReturned() {
    // Given
    String orgIpaCode = "ORG1";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    userInfo.setUserId("user1");
    String accessToken = "token";
    ExportFile exportFile = new ExportFile()
      .exportFileType(ExportFileTypeEnum.PAID)
      .status(ExportFileStatus.COMPLETED)
      .numTotalRows(5L)
      .organizationId(123L)
      .exportFileId(123456L);

    when(exportFileServiceMock.getExportFile(123456L, accessToken)).thenReturn(exportFile);

    // When
    Pair<ExportStatusResponseDTO.StatusEnum, String> result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 123456L, ExportFileTypeEnum.PAID);

    // Then
    assertEquals(ExportStatusResponseDTO.StatusEnum.COMPLETED, result.getLeft());
    assertNotNull(result.getRight());
    assertTrue(result.getRight().contains("123456"));
    assertTrue(result.getRight().contains("123"));
  }
}
