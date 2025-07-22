package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.dto.generated.ImportFileResponseDTO;
import it.gov.pagopa.pu.sil.enums.legacy.IngestionFlowFileLegacyType;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IngestionFlowFileAuthorizationService {
  private static final String FILE_ORIGIN = "SIL";
  private static final String UNKNOWN = "UNKNOWN";

  private final IngestionFlowFileService ingestionFlowFileService;
  private final IngestionFlowFileReservationService ingestionFlowFileReservationService;

  public IngestionFlowFileAuthorizationService(IngestionFlowFileService ingestionFlowFileService,
                                               IngestionFlowFileReservationService ingestionFlowFileReservationService) {
    this.ingestionFlowFileService = ingestionFlowFileService;
    this.ingestionFlowFileReservationService = ingestionFlowFileReservationService;
  }

  public ImportFileResponseDTO authorizeIngestionFlowFile(
    UserInfo userInfo,
    String accessToken,
    String orgIpaCode,
    IngestionFlowFileTypeEnum ingestionFlowFileType
  ) {
    AuthorizationService.validateAdminRole(orgIpaCode, userInfo);

    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);
    IngestionFlowFileRequestDTO requestDTO = mapToReservationRequest(ingestionFlowFileType, organizationId);

    Long ingestionFlowFileId = ingestionFlowFileService.createIngestionFlowFileReservation(requestDTO, accessToken);
    log.debug("Reservation created with ID: {}", ingestionFlowFileId);
    requestDTO.setIngestionFlowFileId(ingestionFlowFileId);
    String uploadUrl = ingestionFlowFileReservationService.generateUploadUrl(requestDTO);
    log.debug("Generated upload URL: {}", uploadUrl);

    return ImportFileResponseDTO.builder()
      .importId(String.valueOf(ingestionFlowFileId))
      .uploadUrl(uploadUrl)
      .build();
  }

  public ImportFileResponseDTO authorizeTreasuryIngestionFlowFile(
    UserInfo userInfo,
    String accessToken,
    String orgIpaCode,
    String ingestionFlowFileLegacyType) {
    IngestionFlowFileTypeEnum ingestionFlowFileTypeEnum = IngestionFlowFileLegacyType.fromLegacyValue2CurrentValue(ingestionFlowFileLegacyType);
    return authorizeIngestionFlowFile(
      userInfo,
      accessToken,
      orgIpaCode,
      ingestionFlowFileTypeEnum
    );
  }

  private IngestionFlowFileRequestDTO mapToReservationRequest(IngestionFlowFileTypeEnum ingestionFlowFileType, Long organizationId) {
    return new IngestionFlowFileRequestDTO()
      .organizationId(organizationId)
      .filePathName(UNKNOWN)
      .fileName(UNKNOWN)
      .fileSize(0L)
      .ingestionFlowFileType(IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum.valueOf(ingestionFlowFileType.toString()))
      .fileOrigin(FILE_ORIGIN);
  }
}
