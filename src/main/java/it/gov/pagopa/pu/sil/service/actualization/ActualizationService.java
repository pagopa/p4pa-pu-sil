package it.gov.pagopa.pu.sil.service.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyActualizationService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import it.gov.pagopa.pu.sil.service.SilAccessTokenService;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ActualizationService {
  private final OrgSilServiceComponent orgSilServiceComponent;
  private final LegacyActualizationService legacyActualizationService;
  private final SilAccessTokenService silAccessTokenService;

  public ActualizationService(OrgSilServiceComponent orgSilServiceComponent,
                              LegacyActualizationService legacyActualizationService,
                              SilAccessTokenService silAccessTokenService) {
    this.orgSilServiceComponent = orgSilServiceComponent;
    this.legacyActualizationService = legacyActualizationService;
    this.silAccessTokenService = silAccessTokenService;
  }

  public ActualizationResultDTO actualize(Long orgSilServiceId, String nav,
                                    UserInfo loggedUser, String accessToken) {
    OrgSilServiceDTO orgSilService = orgSilServiceComponent.getOrgSilServiceById(orgSilServiceId, accessToken)
      .orElseThrow(() -> new IllegalArgumentException("Organization service not found"));
    AuthorizationService.validateUserForOrganizationId(orgSilService.getOrganizationId(), loggedUser);
    String orgFiscalCode = AuthorizationService.getOrgFiscalCodeFromUserInfo(loggedUser, orgSilService.getOrganizationId());

    String silAccessToken = silAccessTokenService.getSilAccessToken(orgSilService, accessToken);

    return legacyActualizationService.actualization(
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
