package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceRequestBodyAuthConfig;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyBasicAuthConfig;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyJwtAuthConfig;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import org.springframework.stereotype.Service;

@Service
public class SilLegacyAuthFacadeService {
  private final SilLegacyBasicAuthService basicAuthService;
  private final SilLegacyJwtAuthService jwtAuthService;

  public SilLegacyAuthFacadeService(SilLegacyBasicAuthService basicAuthService, SilLegacyJwtAuthService jwtAuthService) {
    this.basicAuthService = basicAuthService;
    this.jwtAuthService = jwtAuthService;
  }

  public AccessToken authenticate(RegistryContextData contextData, OrgSilServiceRequestBodyAuthConfig authConfig) {
    return switch (authConfig) {
      case SilServiceLegacyBasicAuthConfig legacyBasicAuth -> basicAuthService.authenticate(contextData, legacyBasicAuth);
      case SilServiceLegacyJwtAuthConfig legacyJwtAuth -> jwtAuthService.authenticate(legacyJwtAuth);
      default -> throw new IllegalArgumentException("Unsupported auth config type: " + authConfig.getClass().getSimpleName());
    };
  }
}
