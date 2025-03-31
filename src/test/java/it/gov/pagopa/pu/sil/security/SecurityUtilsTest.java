package it.gov.pagopa.pu.sil.security;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SecurityUtilsTest {

  private static void configureSecurityContext(UserInfo expectedUserInfo) {
    SecurityContextHolder.setContext(new SecurityContextImpl(new UsernamePasswordAuthenticationToken(expectedUserInfo, "token")));
  }

  @BeforeEach
  void clearContexts(){
    RequestContextHolder.resetRequestAttributes();
    SecurityContextHolder.clearContext();
  }

//region test getLoggedUser
  @Test
  void whenGetLoggedUserThenReturnIt() {
    // Given
    String expectedMappedExternalUserId = "USERID";
    UserInfo expectedUserInfo = new UserInfo();
    expectedUserInfo.setMappedExternalUserId(expectedMappedExternalUserId);
    configureSecurityContext(expectedUserInfo);

    // When
    UserInfo result = SecurityUtils.getLoggedUser();

    // Then
    Assertions.assertSame(expectedUserInfo, result);
    Assertions.assertEquals(expectedMappedExternalUserId, expectedUserInfo.getMappedExternalUserId());
  }

  @Test
  void givenPuSystemUserAndUserIdWhenGetLoggedUserThenReturnIt() {
    // Given
    String expectedMappedExternalUserId = "ANOTHERUSER";
    UserInfo expectedUserInfo = new UserInfo();
    expectedUserInfo.setMappedExternalUserId(SecurityUtils.SYSTEM_USERID_PREFIX + "ORGIPACODE");
    configureSecurityContext(expectedUserInfo);
    configureXUserIdHeader(expectedMappedExternalUserId);

    // When
    UserInfo result = SecurityUtils.getLoggedUser();

    // Then
    Assertions.assertSame(expectedUserInfo, result);
    Assertions.assertEquals(expectedMappedExternalUserId, expectedUserInfo.getMappedExternalUserId());
  }

  public static void configureXUserIdHeader(String expectedMappedExternalUserId) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(SecurityUtils.HEADER_USER_ID, expectedMappedExternalUserId);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  @Test
  void givenPuSystemUserAndNotUserIdWhenGetLoggedUserThenReturnIt() {
    // Given
    String expectedMappedExternalUserId = SecurityUtils.SYSTEM_USERID_PREFIX + "ORGIPACODE";
    UserInfo expectedUserInfo = new UserInfo();
    expectedUserInfo.setMappedExternalUserId(expectedMappedExternalUserId);
    configureSecurityContext(expectedUserInfo);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

    // When
    UserInfo result = SecurityUtils.getLoggedUser();

    // Then
    Assertions.assertSame(expectedUserInfo, result);
    Assertions.assertEquals(expectedMappedExternalUserId, expectedUserInfo.getMappedExternalUserId());
  }

  @Test
  void givenPuSystemUserAndNotHttpContextWhenGetLoggedUserThenReturnIt() {
    // Given
    String expectedMappedExternalUserId = SecurityUtils.SYSTEM_USERID_PREFIX + "ORGIPACODE";
    UserInfo expectedUserInfo = new UserInfo();
    expectedUserInfo.setMappedExternalUserId(expectedMappedExternalUserId);
    configureSecurityContext(expectedUserInfo);

    // When
    UserInfo result = SecurityUtils.getLoggedUser();

    // Then
    Assertions.assertSame(expectedUserInfo, result);
    Assertions.assertEquals(expectedMappedExternalUserId, expectedUserInfo.getMappedExternalUserId());
  }
//endregion

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
