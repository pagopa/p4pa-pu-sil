package it.gov.pagopa.pu.sil.security;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class SecurityUtilsTest {

  private static void configureSecurityContext(UserInfo expectedUserInfo) {
    SecurityContextHolder.setContext(new SecurityContextImpl(new UsernamePasswordAuthenticationToken(expectedUserInfo, "token")));
  }

  @Test
  void testGetPrincipal() {
    // Given
    UserInfo expectedUserInfo = new UserInfo();
    configureSecurityContext(expectedUserInfo);

    // When
    UserInfo result = SecurityUtils.getLoggedUser();

    // Then
    Assertions.assertSame(expectedUserInfo, result);
  }

  @Test
  void testGetPrincipalRoles() {
    // Given
    Set<String> expectedRoles = Set.of("ROLE");

    UserInfo userInfo = new UserInfo();
    UserOrganizationRoles userOrganizationRoles1 = new UserOrganizationRoles();
    userOrganizationRoles1.setOrganizationIpaCode("ORG");
    userOrganizationRoles1.setRoles(List.of("ROLE"));

    UserOrganizationRoles userOrganizationRoles2 = new UserOrganizationRoles();
    userOrganizationRoles2.setOrganizationIpaCode("ORG2");
    userOrganizationRoles2.setRoles(List.of("ROLE2"));

    userInfo.setOrganizations(List.of(userOrganizationRoles1,userOrganizationRoles2));

    configureSecurityContext(userInfo);
    // When
    Set<String> result1 = SecurityUtils.getLoggedUserRoles("ORG");
    Set<String> result2 = SecurityUtils.getLoggedUserRoles("ORG3");

    // Then
    Assertions.assertEquals(expectedRoles, result1);
    Assertions.assertEquals(Collections.emptySet(), result2);
  }

  @Test
  void givenEmptySecurityContextThenGetUserInfo(){
    // Given
    SecurityContextHolder.clearContext();
    // When
    UserInfo result = SecurityUtils.getLoggedUser();
    // Then
    Assertions.assertNull(result);
  }

  @Test
  void givenEmptySecurityContextThenGetLoggedUserRoles(){
    // Given
    SecurityContextHolder.clearContext();
    // When
    Set<String> result = SecurityUtils.getLoggedUserRoles("");
    // Then
    Assertions.assertEquals(Collections.emptySet(),result);
  }

  @Test
  void givenSecurityContextThenGetLoggedUserAccessToken(){
    // Given
    UserInfo expectedUserInfo = new UserInfo();
    configureSecurityContext(expectedUserInfo);
    // When
    String result = SecurityUtils.getAccessToken();
    // Then
    Assertions.assertEquals("token",result);
  }

  @Test
  void givenUriWhenRemovePiiFromURIThenOk(){
    String result = SecurityUtils.removePiiFromURI(URI.create("https://host/path?param1=PII&param2=noPII"));
    Assertions.assertEquals("https://host/path?param1=***&param2=***", result);
  }

  @Test
  void givenNullUriWhenRemovePiiFromURIThenOk(){
    Assertions.assertNull(SecurityUtils.removePiiFromURI(null));
  }

}
