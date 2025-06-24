package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AccessTokenService {
  private final AuthAccessTokenFacade authAccessTokenFacade;

  private final Map<Long, Pair<LocalDateTime, AccessToken>> clientId2accessTokensMap = new ConcurrentHashMap<>();

  public AccessTokenService( AuthAccessTokenFacade authAccessTokenFacade) {
      this.authAccessTokenFacade = authAccessTokenFacade;
  }

  public AccessToken getAccessToken(OrgSilService orgSilService, AccessToken accessToken) {
    return clientId2accessTokensMap.compute(orgSilService.getOrgSilServiceId(), (k, v) -> {
      if (v == null || LocalDateTime.now().isAfter(v.getLeft())) {
        LocalDateTime tokenRequestDateTime = LocalDateTime.now();
        AccessToken actualAccessToken = authAccessTokenFacade.retrieveAccessToken(orgSilService, accessToken);
        LocalDateTime expiration = tokenRequestDateTime.plusSeconds(actualAccessToken.getExpiresIn() - 5L);
        return Pair.of(expiration, actualAccessToken);
      } else {
        return v;
      }
    }).getRight();
  }
}
