package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceRequestBodyAuthConfig;

public abstract class AbstractAuthConfigService<T extends OrgSilServiceRequestBodyAuthConfig, R> {
  private final Class<T> clazz;

  protected AbstractAuthConfigService(Class<T> clazz) {
    this.clazz = clazz;
  }

  public AuthConfigChain<T, R> getAuthConfig(OrgSilService orgSilService) {
    OrgSilServiceRequestBodyAuthConfig config = orgSilService.getAuthConfig();
    if (clazz.isInstance(config)) {
      return new AuthConfigChain<>(clazz.cast(config), this);
    }
    throw new IllegalArgumentException("AuthConfig is not of type " + clazz.getSimpleName());
  }

  public abstract R doAuthentication(T config);
}
