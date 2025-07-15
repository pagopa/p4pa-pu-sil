package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.sil.connector.auth.client.AuthnClient;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

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
    return findUserOrganizationRoles(loggedUser, org -> organizationId.equals(org.getOrganizationId()))
      .filter(AuthorizationService::hasAdminRole)
      .isPresent();
  }

  public static boolean isAdminRole(String organizationIpaCode, UserInfo loggedUser) {
    if (loggedUser == null) {
      return false;
    }
    return findUserOrganizationRoles(loggedUser, org -> organizationIpaCode.equals(org.getOrganizationIpaCode()))
      .filter(AuthorizationService::hasAdminRole)
      .isPresent();
  }

  public static void validateUserForOrganizationId(Long organizationId, UserInfo loggedUser) {
    if (findUserOrganizationRoles(loggedUser, org -> organizationId.equals(org.getOrganizationId())).isEmpty()) {
      handleUnauthorizedUser(organizationId, loggedUser);
    }
  }

  // Organization property extraction methods - keeping original method names for compatibility
  public static String getOrgIpaCodeFromUserInfo(UserInfo loggedUser, Long organizationId) {
    return extractOrganizationProperty(loggedUser, organizationId, null, null,
      UserOrganizationRoles::getOrganizationIpaCode);
  }

  public static String getOrgIpaCodeFromUserInfo(UserInfo loggedUser, String organizationFiscalCode) {
    return extractOrganizationProperty(loggedUser, null, null, organizationFiscalCode,
      UserOrganizationRoles::getOrganizationIpaCode);
  }

  public static String getOrgFiscalCodeFromUserInfo(UserInfo loggedUser, Long organizationId) {
    return extractOrganizationProperty(loggedUser, organizationId, null, null,
      UserOrganizationRoles::getOrganizationFiscalCode);
  }

  public static String getOrgFiscalCodeFromUserInfo(UserInfo loggedUser, String organizationIpaCode) {
    return extractOrganizationProperty(loggedUser, null, organizationIpaCode, null,
      UserOrganizationRoles::getOrganizationFiscalCode);
  }

  public static Long getOrganizationIdFromUserInfo(UserInfo loggedUser, String organizationIpaCode) {
    return extractOrganizationProperty(loggedUser, null, organizationIpaCode, null,
      UserOrganizationRoles::getOrganizationId);
  }

  public static Long getOrganizationIdFromOrgFiscalCode(UserInfo loggedUser, String organizationFiscalCode) {
    return extractOrganizationProperty(loggedUser, null, null, organizationFiscalCode,
      UserOrganizationRoles::getOrganizationId);
  }

  private static <T> T extractOrganizationProperty(UserInfo loggedUser, Long organizationId,
      String organizationIpaCode, String organizationFiscalCode,
      Function<UserOrganizationRoles, T> propertyExtractor) {

    if (loggedUser == null) {
      return null;
    }

    Optional<UserOrganizationRoles> orgRoles = Optional.empty();
    if (organizationId != null) {
      orgRoles = findUserOrganizationRoles(loggedUser, org -> organizationId.equals(org.getOrganizationId()));
    } else if (organizationIpaCode != null) {
      orgRoles = findUserOrganizationRoles(loggedUser, org -> organizationIpaCode.equals(org.getOrganizationIpaCode()));
    } else if (organizationFiscalCode != null) {
      orgRoles = findUserOrganizationRoles(loggedUser, org -> organizationFiscalCode.equals(org.getOrganizationFiscalCode()));
    }
    return orgRoles.map(propertyExtractor).orElse(null);
  }

  private static Optional<UserOrganizationRoles> findUserOrganizationRoles(UserInfo loggedUser,
      Predicate<UserOrganizationRoles> predicate) {

    if (loggedUser == null || CollectionUtils.isEmpty(loggedUser.getOrganizations())) {
      return Optional.empty();
    }

    return loggedUser.getOrganizations().stream()
      .filter(Objects::nonNull)
      .filter(org -> predicate.test(org) && hasValidRoles(org))
      .findFirst();
  }

  private static boolean hasValidRoles(UserOrganizationRoles organization) {
    return !CollectionUtils.isEmpty(organization.getRoles());
  }

  private static boolean hasAdminRole(UserOrganizationRoles organization) {
    return hasValidRoles(organization) && organization.getRoles().contains(ROLE_ADMIN);
  }

  private static void handleUnauthorizedUser(Long organizationId, UserInfo loggedUser) {
    log.debug("Unauthorized user. [organizationId:{}]", organizationId);
    String userId = loggedUser != null ? loggedUser.getMappedExternalUserId() : "unknown";
    throw new AuthorizationDeniedException("Access denied on organizationId " + organizationId + " to user " + userId);
  }
}
