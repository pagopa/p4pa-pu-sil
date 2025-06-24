package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.service.legacyauth.SilLegacyAuthFacadeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthAccessTokenFacade {
  private final SilLegacyAuthFacadeService silLegacyAuthFacadeService;

  public AuthAccessTokenFacade(SilLegacyAuthFacadeService silLegacyAuthFacadeService) {
    this.silLegacyAuthFacadeService = silLegacyAuthFacadeService;
  }

  public AccessToken retrieveAccessToken(OrgSilService orgSilService, AccessToken accessToken) {
    if (orgSilService.getFlagLegacy().equals(Boolean.TRUE)) {
      log.info("retrieve legacy authentication for orgSilServiceId: {}", orgSilService.getOrgSilServiceId());
      return silLegacyAuthFacadeService.authenticate(orgSilService.getAuthConfig());
    } else {
      log.info("Using current access token {} for orgSilServiceId: {}", accessToken, orgSilService.getOrgSilServiceId());
      return accessToken;
    }
  }
}

