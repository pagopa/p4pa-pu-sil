package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.exception.IngestionFlowFileTypeNotValidException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IngestionFlowFileAuthorizationServiceTest {

  @Mock
  private IngestionFlowFileService ingestionFlowFileServiceMock;
  @Mock
  private IngestionFlowFileReservationService ingestionFlowFileReservationServiceMock;

  private IngestionFlowFileAuthorizationService service;

  @BeforeEach
  void setUp() {
    service = new IngestionFlowFileAuthorizationService(
      ingestionFlowFileServiceMock,
      ingestionFlowFileReservationServiceMock
    );
  }

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(
      ingestionFlowFileServiceMock,
      ingestionFlowFileReservationServiceMock
    );
  }

  @Test
  void givenUserNotAdminWhenAuthorizeIngestionFlowFileThenKo() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum type = IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum.DP_INSTALLMENTS;

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(false);

      assertThrows(UnauthorizedException.class, () ->
        service.authorizeIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          type
        )
      );
      verifyNoInteractions(ingestionFlowFileServiceMock, ingestionFlowFileReservationServiceMock);
    }
  }

  @Test
  void givenUserIsAdminWhenAuthorizeIngestionFlowFileThenOk() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin1");
    String orgIpaCode = "ORG2";
    String accessToken = "token2";
    IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum type = IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum.DP_INSTALLMENTS;

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(123L);

      when(ingestionFlowFileServiceMock.createIngestionFlowFileReservation(any(), eq(accessToken))).thenReturn(456L);
      when(ingestionFlowFileReservationServiceMock.generateUploadUrl(any())).thenReturn("http://upload.url");

      Pair<Long, String> result = service.authorizeIngestionFlowFile(
        userInfo,
        accessToken,
        orgIpaCode,
        type
      );

      assertNotNull(result);
      assertEquals(456L, result.getLeft());
      assertEquals("http://upload.url", result.getRight());
      verify(ingestionFlowFileServiceMock).createIngestionFlowFileReservation(any(), eq(accessToken));
      verify(ingestionFlowFileReservationServiceMock).generateUploadUrl(any());
    }
  }

  @Test
  void givenNullUserInfoWhenAuthorizeIngestionFlowFileThenOk() {
    UserInfo userInfo = null;
    String orgIpaCode = "ORG3";
    String accessToken = "token3";
    IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum type = IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum.DP_INSTALLMENTS;

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), isNull())).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(isNull(), eq(orgIpaCode))).thenReturn(789L);

      when(ingestionFlowFileServiceMock.createIngestionFlowFileReservation(any(), eq(accessToken))).thenReturn(999L);
      when(ingestionFlowFileReservationServiceMock.generateUploadUrl(any())).thenReturn("http://upload.null");

      Pair<Long, String> result = service.authorizeIngestionFlowFile(
        userInfo,
        accessToken,
        orgIpaCode,
        type
      );

      assertNotNull(result);
      assertEquals(999L, result.getLeft());
      assertEquals("http://upload.null", result.getRight());
      verify(ingestionFlowFileServiceMock).createIngestionFlowFileReservation(any(), eq(accessToken));
      verify(ingestionFlowFileReservationServiceMock).generateUploadUrl(any());
    }
  }

  @Test
  void givenUploadUrlNullWhenAuthorizeIngestionFlowFileThenOk() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin2");
    String orgIpaCode = "ORG4";
    String accessToken = "token4";
    IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum type = IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum.DP_INSTALLMENTS;

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(321L);

      when(ingestionFlowFileServiceMock.createIngestionFlowFileReservation(any(), eq(accessToken))).thenReturn(654L);
      when(ingestionFlowFileReservationServiceMock.generateUploadUrl(any())).thenReturn(null);

      Pair<Long, String> result =
        service.authorizeIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          type
        );

      assertNotNull(result);
      assertEquals(654L, result.getLeft());
      assertNull(result.getRight());
      verify(ingestionFlowFileServiceMock).createIngestionFlowFileReservation(any(), eq(accessToken));
      verify(ingestionFlowFileReservationServiceMock).generateUploadUrl(any());
    }
  }

  @Test
  void givenUserIsAdminWhenAuthorizeTreasuryIngestionFlowFileThenOk() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin1");
    String orgIpaCode = "ORG2";
    String accessToken = "token2";
    IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum type = IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum.TREASURY_OPI;

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      authMock.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode))).thenReturn(123L);

      when(ingestionFlowFileServiceMock.createIngestionFlowFileReservation(any(), eq(accessToken))).thenReturn(456L);
      when(ingestionFlowFileReservationServiceMock.generateUploadUrl(any())).thenReturn("http://upload.url");

      Pair<Long, String> result = service.authorizeTreasuryIngestionFlowFile(
        userInfo,
        accessToken,
        orgIpaCode,
        type
      );

      assertNotNull(result);
      assertEquals(456L, result.getLeft());
      assertEquals("http://upload.url", result.getRight());
      verify(ingestionFlowFileServiceMock).createIngestionFlowFileReservation(any(), eq(accessToken));
      verify(ingestionFlowFileReservationServiceMock).generateUploadUrl(any());
    }
  }

  @Test
  void givenUserIsAdminAndWrongTypeWhenAuthorizeTreasuryIngestionFlowFileThenThrowsIngestionFlowFileTypeNotValidException() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("admin1");
    String orgIpaCode = "ORG2";
    String accessToken = "token2";
    IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum type = IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum.DP_INSTALLMENTS;

    assertThrows(IngestionFlowFileTypeNotValidException.class,
      () -> service.authorizeTreasuryIngestionFlowFile(
        userInfo,
        accessToken,
        orgIpaCode,
        type
      ));
  }
}
