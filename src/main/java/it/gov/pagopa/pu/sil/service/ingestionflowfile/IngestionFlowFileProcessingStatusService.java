package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.dto.generated.DownloadUrl;
import it.gov.pagopa.pu.sil.dto.generated.DownloadUrl.CodeEnum;
import it.gov.pagopa.pu.sil.dto.generated.ImportStatusResponseDTO;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
public class IngestionFlowFileProcessingStatusService {
  private final String fileShareBaseUrl;
  private final IngestionFlowFileService ingestionFlowFileService;

  public IngestionFlowFileProcessingStatusService(@Value("${public-base-url.fileshare}") String fileShareBaseUrl,
                                                  IngestionFlowFileService ingestionFlowFileService) {
    this.fileShareBaseUrl = fileShareBaseUrl;
    this.ingestionFlowFileService = ingestionFlowFileService;
  }

  public ImportStatusResponseDTO getProcessingStatus(
    UserInfo userInfo,
    String accessToken,
    String orgIpaCode,
    Long ingestionFlowFileId,
    IngestionFlowFile.IngestionFlowFileTypeEnum... expectedTypes) {

    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);
    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call ingestion flow file for organization {}", clientId, orgIpaCode);
      throw new UnauthorizedException("Utente non autorizzato");
    }

    IngestionFlowFile ingestionFlowFile = ingestionFlowFileService.getIngestionFlowFile(ingestionFlowFileId, accessToken);
    log.debug("Retrieved IngestionFlowFile: {}", ingestionFlowFile);

    verifyMatchingTypes(ingestionFlowFile, expectedTypes);

    ImportStatusResponseDTO responseDTO = ImportStatusResponseDTO.builder()
      .status(ingestionFlowFile.getStatus())
      .build();

    if (IngestionFlowFileStatus.COMPLETED.equals(ingestionFlowFile.getStatus())) {
      addDownloadUrlsToResponse(responseDTO, ingestionFlowFile);
    }
    return responseDTO;
  }

  private void verifyMatchingTypes(IngestionFlowFile ingestionFlowFile, IngestionFlowFileTypeEnum... expectedTypes) {
    if (expectedTypes == null || expectedTypes.length == 0) {
      return;
    }
    if (Arrays.stream(expectedTypes).noneMatch(type -> type == ingestionFlowFile.getIngestionFlowFileType())) {
      throw new IllegalArgumentException("Type mismatch: expected %s but found %s"
        .formatted(Arrays.toString(expectedTypes), ingestionFlowFile.getIngestionFlowFileType()));
    }
  }

  private void addDownloadUrlsToResponse(ImportStatusResponseDTO responseDTO, IngestionFlowFile ingestionFlowFile) {
    responseDTO.addDownloadUrlsItem(new DownloadUrl(CodeEnum.OUTPUT_FILE, composeUrl(ingestionFlowFile, "/imported")));
    if (ingestionFlowFile.getDiscardFileName() != null) {
      responseDTO.addDownloadUrlsItem(new DownloadUrl(CodeEnum.DISCARDED_FILE, composeUrl(ingestionFlowFile, "/errors")));
    }
    if (ingestionFlowFile.getPdfGeneratedId() != null) {
      responseDTO.addDownloadUrlsItem(new DownloadUrl(CodeEnum.PAYMENT_NOTICE_FILE, composeUrl(ingestionFlowFile, "/notice")));
    }
  }

  private String composeUrl(IngestionFlowFile ingestionFlowFile, String suffixPath) {
    return UriComponentsBuilder.fromUriString(fileShareBaseUrl)
      .path("/organization/{organizationId}/ingestionflowfiles/{ingestionFlowFileId}")
      .path(suffixPath)
      .buildAndExpand(ingestionFlowFile.getOrganizationId(), ingestionFlowFile.getIngestionFlowFileId())
      .toUriString();
  }
}
