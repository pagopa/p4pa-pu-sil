package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyBasicAuthConfigDTO;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyJwtAuthConfigDTO;
import org.springframework.stereotype.Service;

@Service
public class SilLegacyAuthFacadeService {
  private final SilLegacyBasicAuthService basicAuthService;
  private final SilLegacyJwtAuthService jwtAuthService;

  public SilLegacyAuthFacadeService(SilLegacyBasicAuthService basicAuthService, SilLegacyJwtAuthService jwtAuthService) {
    this.basicAuthService = basicAuthService;
    this.jwtAuthService = jwtAuthService;
  }

  public AccessToken authenticate(String orgFiscalCode, String nav, UserInfo loggedUser, OrgSilServiceDTO orgSilService) {
    return switch (orgSilService.getAuthConfig()) {
      case SilServiceLegacyBasicAuthConfigDTO legacyBasicAuth -> basicAuthService.authenticate(orgFiscalCode, orgSilService.getApplicationName(), nav, loggedUser, legacyBasicAuth);
      case SilServiceLegacyJwtAuthConfigDTO legacyJwtAuth -> jwtAuthService.authenticate(legacyJwtAuth);
      default -> throw new IllegalArgumentException("Unsupported auth config type: " + orgSilService.getClass().getSimpleName());
    };
  }
}
