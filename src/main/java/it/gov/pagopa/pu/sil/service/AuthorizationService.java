package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.auth.client.AuthnClient;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Service
@Slf4j
public class AuthorizationService {
  public static final String ROLE_ADMIN = "ROLE_ADMIN";
  public static final String UNKNOWN = "unknown";

  private final AuthnClient authClientImpl;

  public AuthorizationService(AuthnClient authClientImpl) {
    this.authClientImpl = authClientImpl;
  }

  public UserInfo validateToken(String accessToken) {
    log.info("Requesting validate token");
    return authClientImpl.getUserInfo(accessToken);
  }

  public void validateBrokerAdminRole(UserInfo loggedUser) {
    String brokerFiscalCode = loggedUser.getBrokerFiscalCode();
    boolean isBroker = loggedUser.getOrganizations()
      .stream()
      .anyMatch(o ->
        o.getOrganizationFiscalCode().equals(brokerFiscalCode) &&
          !CollectionUtils.isEmpty(o.getRoles()) &&
          o.getRoles().contains(ROLE_ADMIN));
    if (!isBroker) {
      handleUnauthorizedUserFiscalCode(brokerFiscalCode, loggedUser);
    }
  }

  public void validateOrganizationBrokered(Organization organization, UserInfo loggedUser) {
    if (!isOrganizationHandledByBroker(organization, loggedUser)) {
      handleUnauthorizedUser(organization.getOrganizationId(), loggedUser);
    }
  }

  public boolean isOrganizationHandledByBroker(Organization organization, UserInfo loggedUser) {
    return loggedUser.getBrokerId() != null && organization != null
      && loggedUser.getBrokerId().equals(organization.getBrokerId());
  }

  public static void validateAdminRole(Long organizationId, UserInfo loggedUser) {
    boolean roleAdmin = isAdminRole(organizationId, loggedUser);
    if (!roleAdmin) {
      handleUnauthorizedUser(organizationId, loggedUser);
    }
  }
  public static void validateAdminRole(String organizationIpaCode, UserInfo loggedUser) {
    boolean roleAdmin = isAdminRole(organizationIpaCode, loggedUser);
    if (!roleAdmin) {
      handleUnauthorizedUser(organizationIpaCode, loggedUser);
    }
  }

  public static boolean isAdminRole(Long organizationId, UserInfo loggedUser) {
    return findUserOrganizationRoles(loggedUser, organizationId, null, null)
      .filter(AuthorizationService::hasAdminRole)
      .isPresent();
  }

  public static boolean isAdminRole(String organizationIpaCode, UserInfo loggedUser) {
    if (loggedUser == null) {
      return false;
    }
    return findUserOrganizationRoles(loggedUser, null, organizationIpaCode, null)
      .filter(AuthorizationService::hasAdminRole)
      .isPresent();
  }

  public static void validateUserForOrganizationId(Long organizationId, UserInfo loggedUser) {
    if (findUserOrganizationRoles(loggedUser, organizationId, null, null).isEmpty()) {
      handleUnauthorizedUser(organizationId, loggedUser);
    }
  }

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
    return findUserOrganizationRoles(loggedUser, organizationId, organizationIpaCode, organizationFiscalCode)
      .map(propertyExtractor)
      .orElse(null);
  }

  private static Optional<UserOrganizationRoles> findUserOrganizationRoles(UserInfo loggedUser,
                                                                           Long organizationId,
                                                                           String organizationIpaCode,
                                                                           String organizationFiscalCode) {
    if (loggedUser == null || CollectionUtils.isEmpty(loggedUser.getOrganizations())) {
      return Optional.empty();
    }
    return loggedUser.getOrganizations().stream()
      .filter(Objects::nonNull)
      .filter(org -> hasValidRoles(org) && (
          (organizationId != null && organizationId.equals(org.getOrganizationId())) ||
            (organizationIpaCode != null && organizationIpaCode.equals(org.getOrganizationIpaCode())) ||
            (organizationFiscalCode != null && organizationFiscalCode.equals(org.getOrganizationFiscalCode()))
        )
      )
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
    String userId = loggedUser != null ? loggedUser.getMappedExternalUserId() : UNKNOWN;
    throw new AuthorizationDeniedException("Access denied on organizationId " + organizationId + " to user " + userId);
  }

  private static void handleUnauthorizedUser(String orgIpaCode, UserInfo loggedUser) {
    log.debug("Unauthorized user. [orgIpaCode:{}]", orgIpaCode);
    String userId = loggedUser != null ? loggedUser.getMappedExternalUserId() : UNKNOWN;
    throw new AuthorizationDeniedException("Access denied on orgIpaCode " + orgIpaCode + " to user " + userId);
  }

  private static void handleUnauthorizedUserFiscalCode(String orgFiscalCode, UserInfo loggedUser) {
    log.debug("Unauthorized user. [orgFiscalCode:{}]", orgFiscalCode);
    String userId = loggedUser != null ? loggedUser.getMappedExternalUserId() : UNKNOWN;
    throw new AuthorizationDeniedException("Access denied on orgFiscalCode " + orgFiscalCode + " to user " + userId);
  }
}
