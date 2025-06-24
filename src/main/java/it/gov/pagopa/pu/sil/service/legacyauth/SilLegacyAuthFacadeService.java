package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceRequestBodyAuthConfig;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyBasicAuthConfig;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyJwtAuthConfig;
import org.springframework.stereotype.Service;

@Service
public class SilLegacyAuthFacadeService {
  private final SilLegacyBasicAuthService basicAuthService;
  private final SilLegacyJwtAuthService jwtAuthService;

  public SilLegacyAuthFacadeService(SilLegacyBasicAuthService basicAuthService, SilLegacyJwtAuthService jwtAuthService) {
    this.basicAuthService = basicAuthService;
    this.jwtAuthService = jwtAuthService;
  }

  public <T> T authenticate(OrgSilServiceRequestBodyAuthConfig authConfig) {
    return switch (authConfig) {
      case SilServiceLegacyBasicAuthConfig legacyBasicAuth -> (T) basicAuthService.authenticate(legacyBasicAuth);
      case SilServiceLegacyJwtAuthConfig legacyJwtAuth -> (T) jwtAuthService.authenticate(legacyJwtAuth);
      default -> throw new IllegalArgumentException("Unsupported auth config type: " + authConfig.getClass().getSimpleName());
    };
  }
}
