package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.sil.connector.auth.client.AuthnClient;
import it.gov.pagopa.pu.sil.exception.InvalidAccessTokenException;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthorizationServiceTest {

  @Mock
  private AuthnClient authClientImplMock;

  @InjectMocks
  private AuthorizationService authorizationService;

  public static UserInfo buildAdminUser() {
    return buildAdminUser(1L, "ORGFC", "ORGIPA");
  }

  public static UserInfo buildAdminUser(Long organizationId, String orgFiscalCode, String orgIpaCode) {
    UserOrganizationRoles userAdminRole = new UserOrganizationRoles();
    userAdminRole.setRoles(List.of("TEST","ROLE_ADMIN"));
    userAdminRole.setOrganizationId(organizationId);
    userAdminRole.setOrganizationFiscalCode(orgFiscalCode);
    userAdminRole.setOrganizationIpaCode(orgIpaCode);
    UserOrganizationRoles userTestRole = new UserOrganizationRoles();
    userTestRole.setRoles(List.of("TEST"));
    userTestRole.setOrganizationId(2L);
    UserInfo userInfo = new UserInfo();
    userInfo.setOrganizations(List.of(userAdminRole,userTestRole));
    return userInfo;
  }

  @Test
  void givenValidAccessTokenWhenValidateTokenThenOk() {
    UserInfo ui = new UserInfo();
    when(authClientImplMock.getUserInfo("ACCESSTOKEN")).thenReturn(ui);
    UserInfo result = authorizationService.validateToken("ACCESSTOKEN");

    Assertions.assertEquals(ui, result);
  }

  @Test
  void givenInvalidAccessTokenWhenValidateTokenThenInvalidAccessTokenException() {
    when(authClientImplMock.getUserInfo("INVALIDACCESSTOKEN")).thenThrow(new InvalidAccessTokenException("Bad Access Token provided"));
    InvalidAccessTokenException result = Assertions.assertThrows(InvalidAccessTokenException.class,
      () -> authorizationService.validateToken("INVALIDACCESSTOKEN"));

    Assertions.assertEquals("Bad Access Token provided", result.getMessage());
  }

  @Test
  void givenAdminRoleWhenValidateAdminRoleThenOK() {
    UserInfo userInfo = buildAdminUser();
    AuthorizationService.validateAdminRole(1L,userInfo);
  }

  @Test
  void givenNoAdminRoleWhenValidateAdminRoleThenAuthorizationDeniedException() {
    UserInfo userInfo = buildAdminUser();
    userInfo.setMappedExternalUserId("externalUserId");
    AuthorizationDeniedException result = Assertions.assertThrows(
      AuthorizationDeniedException.class,
      () -> AuthorizationService.validateAdminRole(2L,userInfo));

    Assertions.assertEquals("Access denied on organizationId " + 2L + " to user externalUserId", result.getMessage());
  }

  @Test
  void givenAdminRoleWhenValidateAdminOrgIpaCOdeRoleThenOK() {
    UserInfo userInfo = buildAdminUser();
    AuthorizationService.validateAdminRole("ORGIPA", userInfo);
  }

  @Test
  void givenNoAdminRoleWhenValidateAdminRoleOrgIpaCodeThenAuthorizationDeniedException() {
    UserInfo userInfo = buildAdminUser();
    userInfo.setMappedExternalUserId("externalUserId");
    AuthorizationDeniedException result = Assertions.assertThrows(
      AuthorizationDeniedException.class,
      () -> AuthorizationService.validateAdminRole("OTHERIPACODE",userInfo));

    Assertions.assertEquals("Access denied on orgIpaCode OTHERIPACODE to user externalUserId", result.getMessage());
  }

  @Test
  void givenAdminRoleWhenIsAdminRoleThenOK() {
    UserInfo userInfo = buildAdminUser();
    boolean adminRole = AuthorizationService.isAdminRole(1L, userInfo);

    Assertions.assertTrue(adminRole);
  }

  @Test
  void givenNoAdminRoleWhenIsAdminRoleThenAuthorizationDeniedException() {
    UserInfo userInfo = buildAdminUser();
    userInfo.setMappedExternalUserId("externalUserId");
    boolean adminRole = AuthorizationService.isAdminRole(2L, userInfo);

    Assertions.assertFalse(adminRole);
  }

  @Test
  void givenUserEnabledToOrganizationIdWhenValidateUserForOrganizationIdThenOk() {
    UserOrganizationRoles userOrgRole = new UserOrganizationRoles();
    userOrgRole.setRoles(List.of("TEST"));
    userOrgRole.setOrganizationId(1L);

    UserInfo userInfo = new UserInfo();
    userInfo.setOrganizations(List.of(userOrgRole));

    Assertions.assertDoesNotThrow(() -> AuthorizationService.validateUserForOrganizationId(1L, userInfo));
  }

  @Test
  void givenUserNotEnabledToOrganizationIdWhenValidateUserForOrganizationIdThenUnauthorized() {
    UserOrganizationRoles userOrgRole = new UserOrganizationRoles();
    userOrgRole.setRoles(List.of("TEST"));
    userOrgRole.setOrganizationId(1L);

    UserInfo userInfo = new UserInfo();
    userInfo.setOrganizations(List.of(userOrgRole));

    Assertions.assertThrows(AuthorizationDeniedException.class, () -> AuthorizationService.validateUserForOrganizationId(2L, userInfo));
  }

  @Test
  void givenUserWithEmptyRolesWhenValidateUserForOrganizationIdThenUnauthorized() {
    UserOrganizationRoles userOrgRole = new UserOrganizationRoles();
    userOrgRole.setRoles(List.of());
    userOrgRole.setOrganizationId(1L);

    UserInfo userInfo = new UserInfo();
    userInfo.setOrganizations(List.of(userOrgRole));

    Assertions.assertThrows(AuthorizationDeniedException.class, () -> AuthorizationService.validateUserForOrganizationId(1L, userInfo));
  }

  @Test
  void givenUserWithNullRolesWhenValidateUserForOrganizationIdThenUnauthorized() {
    UserOrganizationRoles userOrgRole = new UserOrganizationRoles();
    userOrgRole.setRoles(null);
    userOrgRole.setOrganizationId(1L);

    UserInfo userInfo = new UserInfo();
    userInfo.setOrganizations(List.of(userOrgRole));

    Assertions.assertThrows(AuthorizationDeniedException.class, () -> AuthorizationService.validateUserForOrganizationId(1L, userInfo));
  }

  @ParameterizedTest
  @CsvSource({
    "true, IPA_2, true",  // Valid admin user for the organization
    "true, IPA_1, false", // User without admin role for the organization
    "true, IPA_3, false", // Organization not associated with the user
    "false, IPA_2, false"  // Invalid user (no logged-in user)
  })
  void testIsAdminRole(boolean logged, String organizationIpaCode, boolean expectedResult) {
    // Given
    UserInfo expectedUserInfo = null;
    if (logged) {
      expectedUserInfo = new UserInfo();
      expectedUserInfo.setMappedExternalUserId("USERID");
      expectedUserInfo.setOrganizations(List.of(
        new UserOrganizationRoles("OID1", 1L, "IPA_1", "CF_1", "email", List.of("")),
        new UserOrganizationRoles("OID2", 2L, "IPA_2", "CF_2", "email", List.of(SecurityUtils.OPERATOR_ROLE_ADMIN))
      ));
    }

    // When
    boolean result = AuthorizationService.isAdminRole(organizationIpaCode, expectedUserInfo);

    // Then
    Assertions.assertEquals(expectedResult, result);
  }


  @ParameterizedTest
  @CsvSource(value={
    "USERID, IPA_1, CF_1",  // Valid organization with fiscal code
    "USERID, IPA_2, CF_2",  // Another valid organization with fiscal code
    "USERID, IPA_3, null",  // Organization not associated with the user
    "null, IPA_1, null",    // Null user
    "USERID, null, null"    // Null organization IPA code
  }, nullValues={"null"})
  void testGetOrgFiscalCodeFromUserInfo(String userId, String organizationIpaCode, String expectedFiscalCode) {
    // Given
    UserInfo userInfo = null;
    if (userId != null) {
      userInfo = new UserInfo();
      userInfo.setMappedExternalUserId(userId);
      userInfo.setOrganizations(List.of(
        new UserOrganizationRoles("OID1", 1L, "IPA_1", "CF_1", "email", List.of("ROLE_USER")),
        new UserOrganizationRoles("OID2", 2L, "IPA_2", "CF_2", "email", List.of("ROLE_ADMIN"))
      ));
    }

    // When
    String result = AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, organizationIpaCode);

    // Then
    Assertions.assertEquals(expectedFiscalCode, result);
  }

  @ParameterizedTest
  @CsvSource(value={
    "USERID, IPA_1, 1",
    "USERID, IPA_2, 2",
    "USERID, IPA_3, null",
    "null, IPA_1, null",
    "USERID, null, null"
  }, nullValues={"null"})
  void testGetOrganizationIdFromUserInfo(String userId, String organizationIpaCode, Long expectedId) {
    // Given
    UserInfo userInfo = null;
    if (userId != null) {
      userInfo = new UserInfo();
      userInfo.setMappedExternalUserId(userId);
      userInfo.setOrganizations(List.of(
        new UserOrganizationRoles("OID1", 1L, "IPA_1", "CF_1", "email", List.of("ROLE_USER")),
        new UserOrganizationRoles("OID2", 2L, "IPA_2", "CF_2", "email", List.of("ROLE_ADMIN"))
      ));
    }

    // When
    Long result = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, organizationIpaCode);

    // Then
    Assertions.assertEquals(expectedId, result);
  }

  @ParameterizedTest
  @CsvSource(value={
    "USERID, 1, IPA_1",  // Valid organization
    "USERID, 2, IPA_2",  // Another valid organization
    "USERID, 3, null",   // Organization not associated with the user
    "null, 1, null",     // Null user
    "USERID, null, null" // Null organizationId
  }, nullValues={"null"})
  void testGetOrgIpaCodeFromUserInfo(String userId, Long organizationId, String expectedIpaCode) {
    // Given
    UserInfo userInfo = null;
    if (userId != null) {
      userInfo = new UserInfo();
      userInfo.setMappedExternalUserId(userId);
      userInfo.setOrganizations(List.of(
        new UserOrganizationRoles("OID1", 1L, "IPA_1", "CF_1", "email", List.of("ROLE_USER")),
        new UserOrganizationRoles("OID2", 2L, "IPA_2", "CF_2", "email", List.of("ROLE_ADMIN"))
      ));
    }

    // When
    String result = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, organizationId);

    // Then
    Assertions.assertEquals(expectedIpaCode, result);
  }

  @ParameterizedTest
  @CsvSource(value={
    "USERID, CF_1, 1",
    "USERID, CF_2, 2",
    "USERID, CF_3, null",
    "null, CF_1, null",
    "USERID, null, null"
  }, nullValues={"null"})
  void testGetOrganizationIdFromOrgFiscalCode(String userId, String organizationFiscalCode, Long expectedId) {
    // Given
    UserInfo userInfo = null;
    if (userId != null) {
      userInfo = new UserInfo();
      userInfo.setMappedExternalUserId(userId);
      userInfo.setOrganizations(List.of(
        new UserOrganizationRoles("OID1", 1L, "IPA_1", "CF_1", "email", List.of("ROLE_USER")),
        new UserOrganizationRoles("OID2", 2L, "IPA_2", "CF_2", "email", List.of("ROLE_ADMIN"))
      ));
    }

    // When
    Long result = AuthorizationService.getOrganizationIdFromOrgFiscalCode(userInfo, organizationFiscalCode);

    // Then
    Assertions.assertEquals(expectedId, result);
  }

  @ParameterizedTest
  @CsvSource(value={
    "USERID, 1, CF_1",  // Valid organization
    "USERID, 2, CF_2",  // Another valid organization
    "USERID, 3, null",   // Organization not associated with the user
    "null, 1, null",     // Null user
    "USERID, null, null" // Null organizationId
  }, nullValues={"null"})
  void testOrgFiscalCodeFromUserInfo(String userId, Long organizationId, String organizationFiscalCode) {
    // Given
    UserInfo userInfo = null;
    if (userId != null) {
      userInfo = new UserInfo();
      userInfo.setMappedExternalUserId(userId);
      userInfo.setOrganizations(List.of(
        new UserOrganizationRoles("OID1", 1L, "IPA_1", "CF_1", "email", List.of("ROLE_USER")),
        new UserOrganizationRoles("OID2", 2L, "IPA_2", "CF_2", "email", List.of("ROLE_ADMIN"))
      ));
    }

    // When
    String result = AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, organizationId);

    // Then
    Assertions.assertEquals(organizationFiscalCode, result);
  }

  @ParameterizedTest
  @CsvSource(value={
    "USERID, CF_1, IPA_1",
    "USERID, CF_2, IPA_2",
    "USERID, CF_3, null",
    "null, CF_1, null",
    "USERID, null, null"
  }, nullValues={"null"})
  void testgetOrgIpaCodeFromUserInfo(String userId, String organizationFiscalCode, String organizationIpaCode) {
    // Given
    UserInfo userInfo = null;
    if (userId != null) {
      userInfo = new UserInfo();
      userInfo.setMappedExternalUserId(userId);
      userInfo.setOrganizations(List.of(
        new UserOrganizationRoles("OID1", 1L, "IPA_1", "CF_1", "email", List.of("ROLE_USER")),
        new UserOrganizationRoles("OID2", 2L, "IPA_2", "CF_2", "email", List.of("ROLE_ADMIN"))
      ));
    }

    // When
    String result = AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, organizationFiscalCode);

    // Then
    Assertions.assertEquals(organizationIpaCode, result);
  }
}
