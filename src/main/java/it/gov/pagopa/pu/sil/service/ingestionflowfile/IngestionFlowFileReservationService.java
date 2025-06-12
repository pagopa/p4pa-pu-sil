package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class IngestionFlowFileReservationService {
  private final String fileShareBaseUrl;

  public IngestionFlowFileReservationService(@Value("${public-base-url.fileshare}") String fileSharePublicBaseUrl) {
    this.fileShareBaseUrl = fileSharePublicBaseUrl;
  }

  public String generateUploadUrl(IngestionFlowFileRequestDTO requestDTO) {
    log.debug("Creating upload URL for IngestionFlowFileRequestDTO: {}", requestDTO);
    return UriComponentsBuilder.fromUriString(fileShareBaseUrl)
      .path("/organization/{organizationId}/ingestionflowfiles")
      .queryParam("ingestionFlowFileId", requestDTO.getIngestionFlowFileId())
      .queryParam("ingestionFlowFileType", requestDTO.getIngestionFlowFileType())
      .queryParam("fileOrigin", requestDTO.getFileOrigin())
      .buildAndExpand(requestDTO.getOrganizationId())
      .toUriString();
  }
}
