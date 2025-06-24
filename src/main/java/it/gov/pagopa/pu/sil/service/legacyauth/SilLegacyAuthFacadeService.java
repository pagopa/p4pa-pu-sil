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
    if (authConfig instanceof SilServiceLegacyBasicAuthConfig) {
      return (T) basicAuthService.authenticate((SilServiceLegacyBasicAuthConfig) authConfig);
    } else if (authConfig instanceof SilServiceLegacyJwtAuthConfig) {
      return (T) jwtAuthService.authenticate((SilServiceLegacyJwtAuthConfig) authConfig);
    } else {
      throw new IllegalArgumentException("Unsupported auth config type: " + authConfig.getClass().getSimpleName());
    }
  }
}
