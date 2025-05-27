package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class IngestionFlowFileReservationServiceImpl implements IngestionFlowFileReservationService {
  private static final String FILE_ORIGIN = "SIL";
  private static final String UNKNOWN = "UNKNOWN";

  private final IngestionFlowFileService ingestionFlowFileService;
  private final String fileShareBaseUrl;

  public IngestionFlowFileReservationServiceImpl(IngestionFlowFileService ingestionFlowFileService,
                                                 @Value("${public-base-url.fileshare}") String fileSharePublicBaseUrl) {
    this.ingestionFlowFileService = ingestionFlowFileService;
    this.fileShareBaseUrl = fileSharePublicBaseUrl;
  }

  @Override
  public String uploadUrlGenerator(IngestionFlowFileTypeEnum ingestionFlowFileType,
                                   Long organizationId,
                                   String accessToken) {

    log.info("Creating ingestion flow file reservation for type: {}, organizationId: {}",
      ingestionFlowFileType, organizationId);

    IngestionFlowFileRequestDTO requestDTO = mapToReservationRequest(ingestionFlowFileType, organizationId);

    Long ingestionFlowFileId = ingestionFlowFileService.createIngestionFlowFileReservation(requestDTO, accessToken);
    log.debug("Reservation created with ID: {}", ingestionFlowFileId);
    requestDTO.setIngestionFlowFileId(ingestionFlowFileId);

    String uploadUrl = composeUploadUrl(requestDTO);
    log.info("Generated upload URL: {}", uploadUrl);

    return uploadUrl;
  }

  private IngestionFlowFileRequestDTO mapToReservationRequest(IngestionFlowFileTypeEnum ingestionFlowFileType, Long organizationId) {
    return new IngestionFlowFileRequestDTO()
      .organizationId(organizationId)
      .filePathName(UNKNOWN)
      .fileName(UNKNOWN)
      .fileSize(0L)
      .ingestionFlowFileType(ingestionFlowFileType)
      .fileOrigin(FILE_ORIGIN);
  }

  private String composeUploadUrl(IngestionFlowFileRequestDTO requestDTO) {
    return UriComponentsBuilder.fromUriString(fileShareBaseUrl)
      .path("/organization/{organizationId}/ingestionflowfiles")
      .queryParam("ingestionFlowFileId", requestDTO.getIngestionFlowFileId())
      .queryParam("ingestionFlowFileType", requestDTO.getIngestionFlowFileType())
      .queryParam("fileOrigin", requestDTO.getFileOrigin())
      .queryParam("fileName", requestDTO.getFileName())
      .buildAndExpand(requestDTO.getOrganizationId())
      .toUriString();
  }
}
