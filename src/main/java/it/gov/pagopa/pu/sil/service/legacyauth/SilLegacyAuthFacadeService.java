package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTOAuthConfig;
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

  public AccessToken authenticate(RegistryContextData contextData, OrgSilServiceDTOAuthConfig authConfig) {
    return switch (authConfig) {
      case SilServiceLegacyBasicAuthConfigDTO legacyBasicAuth -> basicAuthService.authenticate(contextData, legacyBasicAuth);
      case SilServiceLegacyJwtAuthConfigDTO legacyJwtAuth -> jwtAuthService.authenticate(legacyJwtAuth);
      default -> throw new IllegalArgumentException("Unsupported auth config type: " + authConfig.getClass().getSimpleName());
    };
  }
}
