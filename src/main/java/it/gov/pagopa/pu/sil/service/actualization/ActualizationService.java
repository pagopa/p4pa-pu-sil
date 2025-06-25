package it.gov.pagopa.pu.sil.service.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyActualizationService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.dto.generated.AmountUpdatesDTO;
import it.gov.pagopa.pu.sil.service.AccessTokenService;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
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

  public AmountUpdatesDTO actualize(Long orgSilServiceId, String nav,
                                    UserInfo loggedUser, String accessToken) {
    OrgSilService orgSilService = orgSilServiceComponent.getOrgSilServiceById(orgSilServiceId, accessToken)
      .orElseThrow(() -> new IllegalArgumentException("Organization service not found"));
    AuthorizationService.validateUserForOrganizationId(orgSilService.getOrganizationId(), loggedUser);
    String orgFiscalCode = AuthorizationService.getOrgFiscalCodeFromUserInfo(loggedUser, orgSilService.getOrganizationId());

    AccessToken actualAccessToken = accessTokenService.getAccessToken(orgSilService, accessToken);

    AmountUpdatesDTO amountUpdatesDTO = legacyActualizationService.actualization(
      actualAccessToken.getAccessToken(),
      orgSilService.getServiceUrl(),
      Pagamento.builder()
        .importoPosizione(Pagamento.ImportoPosizioneEnum.S)
        .numeroAvviso(nav)
        .cfEnteCreditore(orgFiscalCode)
        .build()
    );

    if (amountUpdatesDTO.getErrorCode() != null) {
      boolean isBlocking = "004".equals(amountUpdatesDTO.getErrorCode());
      amountUpdatesDTO.isBlockingError(isBlocking);
      log.error("{} for orgSilServiceId: {}, orgFiscalCode: {}, nav: {}. Error code: {} - Error message: {}",
        isBlocking ? "Debt position not payable" : "Error during actualization",
        orgSilServiceId, orgFiscalCode, nav, amountUpdatesDTO.getErrorCode(), amountUpdatesDTO.getErrorDescription());
    }
    return amountUpdatesDTO;
  }
}
