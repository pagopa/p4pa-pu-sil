package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.sil.controller.generated.DebtPositionApi;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.singleimport.DebtPositionCreationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class DebtPositionController implements DebtPositionApi {
  private final RegistryLogger registryLogger;
  private final DebtPositionCreationService debtPositionCreationService;

  public DebtPositionController(RegistryLogger registryLogger,
                                DebtPositionCreationService debtPositionCreationService) {
    this.registryLogger = registryLogger;
    this.debtPositionCreationService = debtPositionCreationService;
  }

  @Override
  public ResponseEntity<DebtPositionDTO> createSingleDebtPosition(String orgFiscalCode,
                                                                  it.gov.pagopa.pu.sil.dto.generated.DebtPositionDTO debtPositionDTO) {
    log.info("Creating single debt position for organization: {}", orgFiscalCode);
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode);
    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.PTDP_paaSILImportaDovuto)
      .loggedUser(userInfo)
      .build();

    return ResponseEntity.ok(
      registryLogger.execute(
        contextData,
        debtPositionDTO,
        () -> debtPositionCreationService.handleInsert(
          debtPositionDTO,
          orgIpaCode,
          userInfo,
          accessToken
        ),
        null
      )
    );
  }
}
