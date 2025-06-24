package it.gov.pagopa.pu.sil.connector.auth.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.auth.client.AuthnClient;
import it.gov.pagopa.pu.sil.service.legacyauth.SilLegacyAuthFacadeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AuthAccessTokenRetriever {

  private static final String GRANT_TYPE = "client_credentials";
  private static final String SCOPE = "openid";
  private static final String CLIENT_ID_PREFIX = "piattaforma-unitaria_";

  private final AuthnClient authnClient;
  private final SilLegacyAuthFacadeService silLegacyAuthFacadeService;
  private final String clientSecret;

  private final Map<String, Pair<LocalDateTime, AccessToken>> clientId2accessTokensMap = new ConcurrentHashMap<>();

  public AuthAccessTokenRetriever(
    AuthnClient authnClient,
    SilLegacyAuthFacadeService silLegacyAuthFacadeService,
    @Value("${rest.auth.post-token.client_secret}") String clientSecret) {
    this.authnClient = authnClient;
    this.clientSecret = clientSecret;
    this.silLegacyAuthFacadeService = silLegacyAuthFacadeService;
  }

  public AccessToken getAccessToken(OrgSilService orgSilService, String orgIpaCode) {
    String clientId = CLIENT_ID_PREFIX + StringUtils.stripToEmpty(orgIpaCode);
    return clientId2accessTokensMap.compute(clientId, (k, v) -> {
      if (v == null || LocalDateTime.now().isAfter(v.getLeft())) {
        log.info("M2M AccessToken with clientId[{}] expired, refreshing", clientId);
        LocalDateTime tokenRequestDateTime = LocalDateTime.now();
        AccessToken accessToken;
        if (orgSilService.getFlagLegacy().equals(Boolean.TRUE)) {
          accessToken = silLegacyAuthFacadeService.authenticate(orgSilService.getAuthConfig());
        } else {
          accessToken = authnClient.postToken(clientId, GRANT_TYPE, SCOPE, null, null, null, clientSecret);
        }
        LocalDateTime expiration = tokenRequestDateTime.plusSeconds(accessToken.getExpiresIn() - 5L); // setting some seconds to avoid too strict expiration
        return Pair.of(expiration, accessToken);
      } else {
        return v;
      }
    }).getRight();
  }
}
