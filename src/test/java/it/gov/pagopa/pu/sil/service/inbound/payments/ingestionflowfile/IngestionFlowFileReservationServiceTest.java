package it.gov.pagopa.pu.sil.service.inbound.payments.ingestionflowfile;

import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IngestionFlowFileReservationServiceTest {
  private static final String FILE_ORIGIN = "SIL";
  private static final String UNKNOWN = "UNKNOWN";
  private final String baseUrl = "baseUrl";

  private IngestionFlowFileReservationService service;

  @BeforeEach
  void setUp() {
    service = new IngestionFlowFileReservationService(baseUrl);
  }

  @Test
  void whenUploadUrlGeneratorThenOk() {
    // Given
    Long ingestionFlowFileId = 1L;
    Long organizationId = 123L;
    IngestionFlowFileTypeEnum fileType = IngestionFlowFileTypeEnum.ORGANIZATIONS_SIL_SERVICE;
    IngestionFlowFileRequestDTO requestDTO = new IngestionFlowFileRequestDTO()
            .organizationId(organizationId)
            .ingestionFlowFileId(ingestionFlowFileId)
            .ingestionFlowFileType(fileType)
            .fileOrigin(FILE_ORIGIN)
            .filePathName(UNKNOWN)
            .fileName(UNKNOWN)
            .fileSize(0L);
    String expectedUrl = baseUrl +
                        "/organization/" + organizationId +
                        "/ingestionflowfiles" +
                        "?ingestionFlowFileId=" + ingestionFlowFileId +
                        "&ingestionFlowFileType=" + fileType +
                        "&fileOrigin=SIL";

    // When
    String resultUrl = service.generateUploadUrl(requestDTO);

    // Then
    assertEquals(expectedUrl, resultUrl);
  }
}
