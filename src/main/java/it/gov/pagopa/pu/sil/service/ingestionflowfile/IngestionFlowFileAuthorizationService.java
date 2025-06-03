package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.SilOutcome;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.util.soap.FaultUtils;
import it.veneto.regione.pagamenti.ente.PaaSILAutorizzaImportFlussoRisposta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

  public Triple<PaaSILAutorizzaImportFlussoRisposta, String, SilOutcome> authorizeIngestionFlowFile(UserInfo userInfo, String accessToken, String orgIpaCode, IngestionFlowFileTypeEnum ingestionFlowFileType) {
    PaaSILAutorizzaImportFlussoRisposta response = new PaaSILAutorizzaImportFlussoRisposta();

    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);
    // Check if the logged user has the right to call this endpoint
    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call ingestion flow file for organization {}", clientId, orgIpaCode);
      return Triple.of(FaultUtils.setFaultOnResponse(response, SilFaults.PAA_ENTE_NON_VALIDO, "Utente non autorizzato"), null, SilOutcome.KO);
    }
    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);

    IngestionFlowFileRequestDTO requestDTO = mapToReservationRequest(ingestionFlowFileType, organizationId);

    Long ingestionFlowFileId = ingestionFlowFileService.createIngestionFlowFileReservation(requestDTO, accessToken);
    log.debug("Reservation created with ID: {}", ingestionFlowFileId);
    requestDTO.setIngestionFlowFileId(ingestionFlowFileId);
    String uploadUrl = ingestionFlowFileReservationService.generateUploadUrl(requestDTO);
    log.debug("Generated upload URL: {}", uploadUrl);

    response.setRequestToken(String.valueOf(ingestionFlowFileId));
    response.setUploadUrl(uploadUrl);
    return Triple.of(response, null, SilOutcome.OK);
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
