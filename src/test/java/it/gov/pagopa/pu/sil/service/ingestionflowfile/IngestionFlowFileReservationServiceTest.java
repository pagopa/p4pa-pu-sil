package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionFlowFileReservationServiceTest {
  @Mock
  IngestionFlowFileService ingestionFlowFileServiceMock;
  private IngestionFlowFileReservationService service;
  private final String baseUrl = "baseUrl";

  @BeforeEach
  void setUp() {
    service = new IngestionFlowFileReservationServiceImpl(ingestionFlowFileServiceMock, baseUrl);
  }

  @Test
  void whenUploadUrlGeneratorThenOk() {
    // Given
    String accessToken = "accessToken";
    Long ingestionFlowFileId = 1L;
    Long organizationId = 123L;
    IngestionFlowFileTypeEnum fileType = IngestionFlowFileTypeEnum.ORGANIZATIONS_SIL_SERVICE;

    // Capture the request DTO being sent to the service
    ArgumentCaptor<IngestionFlowFileRequestDTO> requestCaptor = ArgumentCaptor.forClass(IngestionFlowFileRequestDTO.class);

    when(ingestionFlowFileServiceMock.createIngestionFlowFileReservation(requestCaptor.capture(), eq(accessToken)))
      .thenReturn(ingestionFlowFileId);

    // When
    String resultUrl = service.uploadUrlGenerator(fileType, organizationId, accessToken);

    // Then
    // Verify the correct request was made
    IngestionFlowFileRequestDTO capturedRequest = requestCaptor.getValue();
    assertEquals(organizationId, capturedRequest.getOrganizationId());
    assertEquals("UNKNOWN", capturedRequest.getFilePathName());
    assertEquals("UNKNOWN", capturedRequest.getFileName());
    assertEquals(0L, capturedRequest.getFileSize());
    assertEquals(fileType, capturedRequest.getIngestionFlowFileType());
    assertEquals("SIL", capturedRequest.getFileOrigin());

    // Verify the URL is constructed correctly
    String expectedUrl = baseUrl +
                        "/organization/" + organizationId +
                        "/ingestionflowfiles" +
                        "?ingestionFlowFileId=" + ingestionFlowFileId +
                        "&ingestionFlowFileType=" + fileType +
                        "&fileOrigin=SIL" +
                        "&fileName=UNKNOWN";

    assertEquals(expectedUrl, resultUrl);
  }
}
