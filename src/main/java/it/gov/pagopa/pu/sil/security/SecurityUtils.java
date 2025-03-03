package it.gov.pagopa.pu.sil.security;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityUtils {

  private SecurityUtils() {
  }

  /**
   * It will return user's session data from ThreadLocal
   */
  public static UserInfo getLoggedUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      Object principal = authentication.getPrincipal();
      if (principal instanceof UserInfo userInfo) {
        return userInfo;
      }
    }
    return null;
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

  public static String getAccessToken(){
    return SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
  }

  public static String removePiiFromURI(URI uri){
    return uri != null
      ? uri.toString().replaceAll("=[^&]*", "=***")
      : null;
  }

}
