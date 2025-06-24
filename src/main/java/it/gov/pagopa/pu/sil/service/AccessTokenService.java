package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AccessTokenService {
  private static final String CLIENT_ID_PREFIX = "piattaforma-unitaria_";

  private final AuthAccessTokenFacade authAccessTokenFacade;

  private final Map<String, Pair<LocalDateTime, AccessToken>> clientId2accessTokensMap = new ConcurrentHashMap<>();

  public AccessTokenService( AuthAccessTokenFacade authAccessTokenFacade) {
      this.authAccessTokenFacade = authAccessTokenFacade;
  }

  public AccessToken getAccessToken(OrgSilService orgSilService, String orgIpaCode) {
    String clientId = CLIENT_ID_PREFIX + StringUtils.stripToEmpty(orgIpaCode);
    return clientId2accessTokensMap.compute(clientId, (k, v) -> {
      if (v == null || LocalDateTime.now().isAfter(v.getLeft())) {
        log.info("M2M AccessToken with clientId[{}] expired, refreshing", clientId);
        LocalDateTime tokenRequestDateTime = LocalDateTime.now();
        AccessToken accessToken = authAccessTokenFacade.retrieveAccessToken(orgSilService, clientId);
        LocalDateTime expiration = tokenRequestDateTime.plusSeconds(accessToken.getExpiresIn() - 5L);
        return Pair.of(expiration, accessToken);
      } else {
        return v;
      }
    }).getRight();
  }
}
