package it.gov.pagopa.pu.sil.service.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyActualizationService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.dto.generated.AmountUpdatesDTO;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.BalanceService;
import it.gov.pagopa.pu.sil.service.legacyauth.SilLegacyAuthFacadeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ActualizationService {
  private final OrgSilServiceComponent orgSilServiceComponent;
  private final LegacyActualizationService legacyActualizationService;

  public ActualizationService(OrgSilServiceComponent orgSilServiceComponent,
                              LegacyActualizationService legacyActualizationService) {
    this.orgSilServiceComponent = orgSilServiceComponent;
    this.legacyActualizationService = legacyActualizationService;
  }

  public AmountUpdatesDTO actualize(Long orgSilServiceId,
                                    String orgFiscalCode, String nav,
                                    UserInfo loggedUser, String accessToken) {
    Long organizationId = AuthorizationService.getOrganizationIdFromOrgFiscalCode(loggedUser, orgFiscalCode);
    AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser);
    OrgSilService orgSilService = orgSilServiceComponent.getOrgSilServiceById(orgSilServiceId, accessToken)
      .orElseThrow(() -> new IllegalArgumentException("Organization service not found"));

    Token token = new Token();

    AmountUpdatesDTO amountUpdatesDTO = legacyActualizationService.actualization(
      token.getToken(),
      orgSilService.getServiceUrl(),
      Pagamento.builder()
        .importoPosizione(Pagamento.ImportoPosizioneEnum.S)
        .numeroAvviso(nav)
        .cfEnteCreditore(orgFiscalCode)
        .build()
    );

    if (amountUpdatesDTO.getErrorCode() != null) {
      log.error("Error during actualization for orgSilServiceId: {}, orgFiscalCode: {}, nav: {}. Error code: {}",
        orgSilServiceId, orgFiscalCode, nav, amountUpdatesDTO.getErrorCode());
      amountUpdatesDTO.isBlockingError(false);
    }
    if (amountUpdatesDTO.getErrorCode().equals("004")) {
      amountUpdatesDTO.isBlockingError(true);
    }
    return amountUpdatesDTO;
  }
}
