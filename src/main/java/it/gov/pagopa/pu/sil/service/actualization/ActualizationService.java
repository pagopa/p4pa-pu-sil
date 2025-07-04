package it.gov.pagopa.pu.sil.service.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyActualizationService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.service.AccessTokenService;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.util.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ActualizationService {
  private final OrgSilServiceComponent orgSilServiceComponent;
  private final LegacyActualizationService legacyActualizationService;
  private final AccessTokenService accessTokenService;

  public ActualizationService(OrgSilServiceComponent orgSilServiceComponent,
                              LegacyActualizationService legacyActualizationService,
                              AccessTokenService accessTokenService) {
    this.orgSilServiceComponent = orgSilServiceComponent;
    this.legacyActualizationService = legacyActualizationService;
    this.accessTokenService = accessTokenService;
  }

  public ActualizationResultDTO actualize(Long orgSilServiceId, String nav,
                                    UserInfo loggedUser, String accessToken) {
    OrgSilService orgSilService = orgSilServiceComponent.getOrgSilServiceById(orgSilServiceId, accessToken)
      .orElseThrow(() -> new IllegalArgumentException("Organization service not found"));
    AuthorizationService.validateUserForOrganizationId(orgSilService.getOrganizationId(), loggedUser);
    String orgFiscalCode = AuthorizationService.getOrgFiscalCodeFromUserInfo(loggedUser, orgSilService.getOrganizationId());

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.SIL_attualizzazioneImporti)
      .orgSilServiceName(orgSilService.getApplicationName())
      .iuv(Utilities.nav2Iuv(nav))
      .loggedUser(loggedUser)
      .build();

    String silAccessToken = accessTokenService.getAccessToken(contextData, orgSilService, accessToken);

    return legacyActualizationService.actualization(contextData,
      silAccessToken,
      orgSilService.getServiceUrl(),
      Pagamento.builder()
        .importoPosizione(Pagamento.ImportoPosizioneEnum.S)
        .numeroAvviso(nav)
        .cfEnteCreditore(orgFiscalCode)
        .build()
    );
  }
}
