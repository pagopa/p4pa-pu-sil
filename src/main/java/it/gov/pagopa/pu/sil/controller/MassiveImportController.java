package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.controller.generated.MassiveImportApi;
import it.gov.pagopa.pu.sil.dto.generated.ImportFileResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.ImportStatusResponseDTO;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileAuthorizationService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileProcessingStatusService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class MassiveImportController implements MassiveImportApi {
  private final IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationService;
  private final IngestionFlowFileProcessingStatusService ingestionFlowFileProcessingStatusService;
  private final RegistryLogger registryLogger;

  public MassiveImportController(IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationService,
                                 IngestionFlowFileProcessingStatusService ingestionFlowFileProcessingStatusService,
                                 RegistryLogger registryLogger) {
    this.ingestionFlowFileAuthorizationService = ingestionFlowFileAuthorizationService;
    this.ingestionFlowFileProcessingStatusService = ingestionFlowFileProcessingStatusService;
    this.registryLogger = registryLogger;
  }

  @Override
  public ResponseEntity<ImportFileResponseDTO> massiveImportRequest(String orgFiscalCode,
                                                                    IngestionFlowFileTypeEnum fileType) {
    log.info("Received massive import request for orgFiscalCode: {}, fileType: {}", orgFiscalCode, fileType);
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode);
    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.PTDP_paaSILAutorizzaImportFlusso)
      .loggedUser(userInfo)
      .build();

    return ResponseEntity.ok(
      registryLogger.execute(
        contextData,
        fileType,
        () -> Triple.of(ingestionFlowFileAuthorizationService.authorizeIngestionFlowFile(
            userInfo,
            accessToken,
            orgIpaCode,
            fileType
          ),
          null,
          RegistryOutcome.OK),
        null
      )
    );
  }

  @Override
  public ResponseEntity<ImportStatusResponseDTO> massiveImportStatus(String orgFiscalCode, Long importId) {
    log.info("Received massive import status request for orgFiscalCode: {}, importId: {}", orgFiscalCode, importId);
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode);
    return ResponseEntity.ok(ingestionFlowFileProcessingStatusService.getProcessingStatus(
      userInfo,
      accessToken,
      orgIpaCode,
      importId
    ));
  }
}
