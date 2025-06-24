package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.auth.client.AuthnClient;
import it.gov.pagopa.pu.sil.service.legacyauth.SilLegacyAuthFacadeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthAccessTokenFacade {
  private static final String GRANT_TYPE = "client_credentials";
  private static final String SCOPE = "openid";

  private final AuthnClient authnClient;
  private final SilLegacyAuthFacadeService silLegacyAuthFacadeService;
  private final String clientSecret;

  public AuthAccessTokenFacade(AuthnClient authnClient,
                               SilLegacyAuthFacadeService silLegacyAuthFacadeService,
                               @Value("${rest.auth.post-token.client_secret}") String clientSecret) {
    this.authnClient = authnClient;
    this.silLegacyAuthFacadeService = silLegacyAuthFacadeService;
    this.clientSecret = clientSecret;
  }

  public AccessToken retrieveAccessToken(OrgSilService orgSilService, String clientId) {
    if (orgSilService.getFlagLegacy().equals(Boolean.TRUE)) {
      return silLegacyAuthFacadeService.authenticate(orgSilService.getAuthConfig());
    } else {
      return authnClient.postToken(clientId, GRANT_TYPE, SCOPE, null, null, null, clientSecret);
    }
  }
}

