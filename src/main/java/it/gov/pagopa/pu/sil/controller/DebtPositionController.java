package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.sil.controller.generated.DebtPositionApi;
import it.gov.pagopa.pu.sil.dto.ManageDebtPositionWithIudDTO;
import it.gov.pagopa.pu.sil.dto.generated.ManageDebtPositionDTO;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.singleimport.DebtPositionCreationService;
import it.gov.pagopa.pu.sil.service.singleimport.DebtPositionInstallmentsHandlerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class DebtPositionController implements DebtPositionApi {
  private final RegistryLogger registryLogger;
  private final DebtPositionCreationService debtPositionCreationService;
  private final DebtPositionInstallmentsHandlerService debtPositionInstallmentsHandlerService;

  public DebtPositionController(RegistryLogger registryLogger,
                                DebtPositionCreationService debtPositionCreationService,
                                DebtPositionInstallmentsHandlerService debtPositionInstallmentsHandlerService) {
    this.registryLogger = registryLogger;
    this.debtPositionCreationService = debtPositionCreationService;
    this.debtPositionInstallmentsHandlerService = debtPositionInstallmentsHandlerService;
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
        () -> debtPositionCreationService.handleAction(
          debtPositionDTO,
          orgIpaCode,
          userInfo,
          accessToken
        ),
        null
      )
    );
  }

  @Override
  public ResponseEntity<DebtPositionDTO> manageDebtPositionInstallments(String orgFiscalCode, String iud, ManageDebtPositionDTO manageDebtPositionDTO) {
    log.info("Managing debt position installments for organization: {}, iud: {}", orgFiscalCode, iud);
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode);
    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.PTDP_paaSILImportaDovuto)
      .loggedUser(userInfo)
      .iuv(manageDebtPositionDTO.getInstallments().getFirst().getInstallment().getIuv())
      .build();

    return ResponseEntity.ok(
      registryLogger.execute(
        contextData,
        manageDebtPositionDTO,
        () -> debtPositionInstallmentsHandlerService.handleAction(
          //build enriched DTO with IUD
          ManageDebtPositionWithIudDTO.fromManageDebtPositionDTO(manageDebtPositionDTO, iud),
          orgIpaCode,
          userInfo,
          accessToken
        ),
        null
      )
    );
  }
}
