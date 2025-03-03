package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.sil.connector.auth.client.AuthnClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Optional;

@Service
@Slf4j
public class AuthorizationService {
  public static final String ROLE_ADMIN = "ROLE_ADMIN";

  private final AuthnClient authClientImpl;

  public AuthorizationService(AuthnClient authClientImpl) {
    this.authClientImpl = authClientImpl;
  }

  public UserInfo validateToken(String accessToken) {
    log.info("Requesting validate token");
    return authClientImpl.getUserInfo(accessToken);
  }

  public void validateAdminRole(Long organizationId, UserInfo loggedUser) {
    boolean roleAdmin = isAdminRole(organizationId, loggedUser);
    if (!roleAdmin) {
      log.debug("Unauthorized user. [organizationId:{}]", organizationId);
      throw new AuthorizationDeniedException("Access denied on organizationId " + organizationId + " to user " + loggedUser.getMappedExternalUserId());
    }
  }

  public static boolean isAdminRole(Long organizationId, UserInfo loggedUser) {
    return getUserOrganizationRoles(organizationId, loggedUser)
      .filter(o -> !CollectionUtils.isEmpty(o.getRoles()) && o.getRoles()
        .contains(ROLE_ADMIN))
      .isPresent();
  }

  public static boolean isUserEnabledToOrganizationId(Long organizationId, UserInfo loggedUser) {
    return getUserOrganizationRoles(organizationId, loggedUser).isPresent();
  }

  private static Optional<UserOrganizationRoles> getUserOrganizationRoles(Long organizationId, UserInfo loggedUser) {
    return loggedUser.getOrganizations().stream()
      .filter(o -> organizationId.equals(o.getOrganizationId()) && !CollectionUtils.isEmpty(o.getRoles()))
      .findFirst();
  }

}
