package it.gov.pagopa.pu.sil.service.actualization;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.dto.generated.AmountUpdatesDTO;
import it.gov.pagopa.pu.sil.service.AuthorizationService;

public class ActualizationService {
  private final OrgSilServiceComponent orgSilServiceComponent;

  public ActualizationService(OrgSilServiceComponent orgSilServiceComponent) {
    this.orgSilServiceComponent = orgSilServiceComponent;
  }

  public AmountUpdatesDTO actualize(Long orgSilServiceId,
                                    String orgFiscalCode, String nav,
                                    UserInfo loggedUser, String accessToken) {
    Long organizationId = AuthorizationService.getOrganizationIdFromOrgFiscalCode(loggedUser, orgFiscalCode);
    AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser);
    OrgSilService orgSilService = orgSilServiceComponent.getOrgSilServiceById(orgSilServiceId, accessToken)
      .orElseThrow(() -> new IllegalArgumentException("Organization service not found"));


    return new AmountUpdatesDTO();
  }
}
