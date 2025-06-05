package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.IngestionFlowFileTypeNotValidException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.util.soap.FaultUtils;
import it.veneto.regione.pagamenti.pivot.ente.FaultBean;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILAutorizzaImportFlussoTesoreriaRisposta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

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

  public Pair<Long, String> authorizeIngestionFlowFile(
      UserInfo userInfo,
      String accessToken,
      String orgIpaCode,
      IngestionFlowFileTypeEnum ingestionFlowFileType) {

    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);

    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call ingestion flow file for organization {}", clientId, orgIpaCode);
      throw new UnauthorizedException("Utente non autorizzato");
    }

    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);
    IngestionFlowFileRequestDTO requestDTO = mapToReservationRequest(ingestionFlowFileType, organizationId);

    Long ingestionFlowFileId = ingestionFlowFileService.createIngestionFlowFileReservation(requestDTO, accessToken);
    log.debug("Reservation created with ID: {}", ingestionFlowFileId);
    requestDTO.setIngestionFlowFileId(ingestionFlowFileId);
    String uploadUrl = ingestionFlowFileReservationService.generateUploadUrl(requestDTO);
    log.debug("Generated upload URL: {}", uploadUrl);

    return Pair.of(ingestionFlowFileId, uploadUrl);
  }

  public Pair<Long, String> authorizeTreasuryIngestionFlowFile(
      UserInfo userInfo,
      String accessToken,
      String orgIpaCode,
      IngestionFlowFileTypeEnum ingestionFlowFileType) {
    if (!Set.of(
        IngestionFlowFileTypeEnum.TREASURY_OPI,
        IngestionFlowFileTypeEnum.TREASURY_CSV,
        IngestionFlowFileTypeEnum.TREASURY_XLS,
        IngestionFlowFileTypeEnum.TREASURY_POSTE)
      .contains(ingestionFlowFileType)) {
      throw new IngestionFlowFileTypeNotValidException("Tipo di flusso non valido: " + ingestionFlowFileType);
    }
    return authorizeIngestionFlowFile(
      userInfo,
      accessToken,
      orgIpaCode,
      ingestionFlowFileType
    );
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
}
