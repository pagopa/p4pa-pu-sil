package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.dto.PaymentsProcessingStatusDTO;
import it.gov.pagopa.pu.sil.enums.legacy.IngestionFlowFileLegacyStatus;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.veneto.regione.pagamenti.ente.PaaSILChiediStatoImportFlusso;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
public class IngestionFlowFileProcessingStatusService {
  private final IngestionFlowFileService ingestionFlowFileService;
  private final String fileShareBaseUrl;

  @Autowired
  public IngestionFlowFileProcessingStatusService(IngestionFlowFileService ingestionFlowFileService,
                                                  @Value("${public-base-url.fileshare}") String fileShareBaseUrl) {
    this.ingestionFlowFileService = ingestionFlowFileService;
    this.fileShareBaseUrl = fileShareBaseUrl;
  }

  public IngestionFlowFile getIngestionFlowFile(UserInfo userInfo,
                                                String accessToken,
                                                String orgIpaCode,
                                                Long ingestionFlowFileId,
                                                IngestionFlowFileTypeEnum... expectedTypes) {
    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);

    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call ingestion flow file for organization {}", clientId, orgIpaCode);
      throw new UnauthorizedException("Utente non autorizzato");
    }
    IngestionFlowFile ingestionFlowFile = ingestionFlowFileService.getIngestionFlowFile(ingestionFlowFileId, accessToken);
    log.debug("Retrieved IngestionFlowFile: {}", ingestionFlowFile);

    if (Arrays.stream(expectedTypes).noneMatch(ingestionFlowFile.getIngestionFlowFileType()::equals)) {
      throw new IllegalArgumentException("Type mismatch: expected %s but found %s"
        .formatted(expectedTypes, ingestionFlowFile.getIngestionFlowFileType()));
    } else {
      return ingestionFlowFile;
    }
  }

  public PaymentsProcessingStatusDTO getProcessingStatus(PaaSILChiediStatoImportFlusso request,
                                                         UserInfo userInfo,
                                                         String accessToken,
                                                         String orgIpaCode,
                                                         Long ingestionFlowFileId,
                                                         IngestionFlowFileTypeEnum... expectedTypes) {
    IngestionFlowFile ingestionFlowFile = getIngestionFlowFile(userInfo, accessToken, orgIpaCode, ingestionFlowFileId, expectedTypes);
    PaymentsProcessingStatusDTO statusDTO = PaymentsProcessingStatusDTO.builder()
      .status(IngestionFlowFileLegacyStatus.fromValue2LegacyValue(ingestionFlowFile.getStatus()))
      .build();
    if (!IngestionFlowFileStatus.COMPLETED.equals(ingestionFlowFile.getStatus())) {
      log.debug("IngestionFlowFile type {} with ID {} is not completed, returning status only: {}",
        ingestionFlowFile.getStatus(), ingestionFlowFileId, statusDTO);
      return statusDTO;
    }
    return statusDTO.toBuilder()
      .urlErrors(Boolean.TRUE.equals(request.isFileScarti()) ? composeUrl(ingestionFlowFile, "/errors") : null)
      .urlNotice(Boolean.TRUE.equals(request.isFileAvvisi()) ? composeUrl(ingestionFlowFile, "/notice") : null)
      .urlImported(Boolean.TRUE.equals(request.isFileIUV()) ? composeUrl(ingestionFlowFile, "/imported") : null)
      .build();
  }

  private String composeUrl(IngestionFlowFile ingestionFlowFile, String suffixPath) {
    log.debug("Creating download URL for IngestionFlowFile: {}", ingestionFlowFile);
    return UriComponentsBuilder.fromUriString(fileShareBaseUrl)
      .path("/organization/{organizationId}/ingestionflowfiles/{ingestionFlowFileId}")
      .path(suffixPath)
      .buildAndExpand(ingestionFlowFile.getOrganizationId(), ingestionFlowFile.getIngestionFlowFileId())
      .toUriString();
  }
}

