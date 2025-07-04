package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.service.legacyauth.SilLegacyAuthFacadeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AccessTokenService {
  private final SilLegacyAuthFacadeService silLegacyAuthFacadeService;

  private final Map<Long, Pair<LocalDateTime, String>> orgSilServiceId2legacyAccessTokensMap = new ConcurrentHashMap<>();

  public AccessTokenService(SilLegacyAuthFacadeService silLegacyAuthFacadeService) {
      this.silLegacyAuthFacadeService = silLegacyAuthFacadeService;
  }

  public String getAccessToken(OrgSilServiceDTO orgSilService, String loggedUserAccessToken) {
    if (!orgSilService.getFlagLegacy()) {
      log.debug("Using current access token for orgSilServiceId: {}", orgSilService.getOrgSilServiceId());
      return loggedUserAccessToken;
    }
    return orgSilServiceId2legacyAccessTokensMap.compute(orgSilService.getOrgSilServiceId(), (k, v) -> {
      if (v == null || LocalDateTime.now().isAfter(v.getLeft())) {
        log.info("retrieve {} authentication  for orgSilServiceId: {}",
          orgSilService.getAuthConfig().getClass().getSimpleName(),
          orgSilService.getOrgSilServiceId());
        LocalDateTime tokenRequestDateTime = LocalDateTime.now();
        AccessToken accessToken = silLegacyAuthFacadeService.authenticate(orgSilService.getAuthConfig());
        LocalDateTime expiration = tokenRequestDateTime.plusSeconds(accessToken.getExpiresIn() - 5L);
        return Pair.of(expiration, accessToken.getAccessToken());
      } else {
        return v;
      }
    }).getRight();
  }
}
