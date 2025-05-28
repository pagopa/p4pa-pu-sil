package it.gov.pagopa.pu.sil.security;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityUtils {

  private SecurityUtils() {
  }

  public static final String SYSTEM_USERID_PREFIX = "WS_USER-piattaforma-unitaria_";
  public static final String HEADER_USER_ID = "X-user-id";
  public static final String OPERATOR_ROLE_ADMIN = "ROLE_ADMIN";

  /**
   * It will return user's session data from ThreadLocal
   */
  public static UserInfo getLoggedUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      Object principal = authentication.getPrincipal();
      if (principal instanceof UserInfo userInfo) {
        userInfo.setMappedExternalUserId(resolvePuSystemUser(userInfo.getMappedExternalUserId()));
        return userInfo;
      }
    }
    return null;
  }

  public static String resolvePuSystemUser(String mappedExternalUserId) {
    if(mappedExternalUserId != null && mappedExternalUserId.startsWith(SYSTEM_USERID_PREFIX) && RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletRequestAttributes){
      HttpServletRequest requestAttributes = servletRequestAttributes.getRequest();
      mappedExternalUserId = ObjectUtils.firstNonNull(requestAttributes.getHeader(HEADER_USER_ID), mappedExternalUserId);
    }
    return mappedExternalUserId;
  }

  /**
   * It will return user's session roles on requested organization IPA code retrieving it from ThreadLocal
   */
  public static Set<String> getLoggedUserRoles(String organizationIpaCode) {
    UserInfo loggedUser = getLoggedUser();
    if (loggedUser != null) {
      return loggedUser.getOrganizations().stream()
        .filter(org -> org.getOrganizationIpaCode().equals(organizationIpaCode))
        .flatMap(org -> org.getRoles().stream())
        .collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  /**
   * It will return organization info on requested organization IPA code retrieving it from ThreadLocal
   */
  public static UserOrganizationRoles getOrganizationInfoFromLoggedUser(String organizationIpaCode) {
    UserInfo loggedUser = getLoggedUser();
    if (loggedUser != null && organizationIpaCode != null) {
      return loggedUser.getOrganizations().stream()
        .filter(org -> org.getOrganizationIpaCode().equals(organizationIpaCode))
        .findFirst().orElse(null);
    }
    return null;
  }

  public static String getAccessToken(){
    return SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
  }

  public static String removePiiFromURI(URI uri){
    return uri != null
      ? uri.toString().replaceAll("=[^&]*", "=***")
      : null;
  }

  public static boolean isAdminUser(String organizationIpaCode) {
    UserInfo loggedUser = getLoggedUser();
    if (loggedUser != null && organizationIpaCode != null) {
      return loggedUser.getOrganizations().stream()
        .anyMatch(org ->
          org.getOrganizationIpaCode().equals(organizationIpaCode) &&
          org.getRoles().contains(OPERATOR_ROLE_ADMIN));
    }
    return false;
  }

}
