package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.sil.connector.auth.client.AuthnClient;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
      handleUnauthorizedUser(organizationId, loggedUser);
    }
  }

  public static boolean isAdminRole(Long organizationId, UserInfo loggedUser) {
    return getUserOrganizationRoles(organizationId, loggedUser)
      .filter(o -> !CollectionUtils.isEmpty(o.getRoles()) && o.getRoles()
        .contains(ROLE_ADMIN))
      .isPresent();
  }

  public static boolean isAdminRole(String organizationIpaCode, UserInfo loggedUser) {
    return loggedUser != null && getUserOrganizationRoles(organizationIpaCode, loggedUser)
      .filter(o -> !CollectionUtils.isEmpty(o.getRoles()) && o.getRoles()
        .contains(ROLE_ADMIN))
      .isPresent();
  }

  public static void validateUserForOrganizationId(Long organizationId, UserInfo loggedUser) {
    if (getUserOrganizationRoles(organizationId, loggedUser).isEmpty()) {
      handleUnauthorizedUser(organizationId, loggedUser);
    }
  }

  public static String getOrgFiscalCodeFromUserInfo(UserInfo loggedUser, String organizationIpaCode) {
    if(loggedUser == null || organizationIpaCode == null) {
      return null;
    }
    return getUserOrganizationRoles(organizationIpaCode, loggedUser).map(UserOrganizationRoles::getOrganizationFiscalCode)
      .orElse(null);
  }

  public static Long getOrganizationIdFromUserInfo(UserInfo loggedUser, String organizationIpaCode) {
    if(loggedUser == null || organizationIpaCode == null) {
      return null;
    }
    return getUserOrganizationRoles(organizationIpaCode, loggedUser).map(UserOrganizationRoles::getOrganizationId)
      .orElse(null);
  }

  private static void handleUnauthorizedUser(Long organizationId, UserInfo loggedUser) {
    log.debug("Unauthorized user. [organizationId:{}]", organizationId);
    throw new AuthorizationDeniedException("Access denied on organizationId " + organizationId + " to user " + loggedUser.getMappedExternalUserId());
  }

  private static Optional<UserOrganizationRoles> getUserOrganizationRoles(Long organizationId, UserInfo loggedUser) {
    return loggedUser.getOrganizations().stream()
      .filter(o -> organizationId.equals(o.getOrganizationId()) && !CollectionUtils.isEmpty(o.getRoles()))
      .findFirst();
  }

  private static Optional<UserOrganizationRoles> getUserOrganizationRoles(String organizationIpaCode, UserInfo loggedUser) {
    return loggedUser.getOrganizations().stream()
      .filter(o -> organizationIpaCode.equals(o.getOrganizationIpaCode()) && !CollectionUtils.isEmpty(o.getRoles()))
      .findFirst();
  }
}
