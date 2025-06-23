package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceRequestBodyAuthConfig;

public class AuthConfigChain<T extends OrgSilServiceRequestBodyAuthConfig, R> {
    private final T config;
    private final AbstractAuthConfigService<T, R> service;

    public AuthConfigChain(T config, AbstractAuthConfigService<T, R> service) {
        this.config = config;
        this.service = service;
    }

    public R doAuthentication() {
        return service.doAuthentication(config);
    }
}

