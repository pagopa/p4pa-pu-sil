package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.auth.client.AuthnClient;
import it.gov.pagopa.pu.sil.connector.auth.service.AuthnService;
import it.gov.pagopa.pu.sil.service.legacyauth.SilLegacyAuthFacadeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthAccessTokenFacade {
  private static final String CLIENT_ID_PREFIX = "piattaforma-unitaria_";
  private static final String GRANT_TYPE = "client_credentials";
  private static final String SCOPE = "openid";

  private final AuthnService authnService;
  private final SilLegacyAuthFacadeService silLegacyAuthFacadeService;
  private final String clientSecret;

  public AuthAccessTokenFacade(AuthnService authnService,
                               SilLegacyAuthFacadeService silLegacyAuthFacadeService,
                               @Value("${rest.auth.post-token.client_secret}") String clientSecret) {
    this.authnService = authnService;
    this.silLegacyAuthFacadeService = silLegacyAuthFacadeService;
    this.clientSecret = clientSecret;
  }

  public AccessToken retrieveAccessToken(OrgSilService orgSilService, UserInfo userInfo) {
    if (orgSilService.getFlagLegacy().equals(Boolean.TRUE)) {
      return silLegacyAuthFacadeService.authenticate(orgSilService.getAuthConfig());
    } else {
      String clientId = CLIENT_ID_PREFIX + AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgSilService.getOrganizationId());
      log.info("M2M AccessToken with clientId[{}] expired, refreshing", clientId);
      return authnService.postToken(clientId, GRANT_TYPE, SCOPE, null, null, null, clientSecret);
    }
  }
}

