package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.enums.SilOutcome;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.veneto.regione.pagamenti.ente.PaaSILAutorizzaImportFlussoRisposta;
import it.veneto.regione.pagamenti.ente.FaultBean;
import org.apache.commons.lang3.tuple.Triple;
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

      Triple<PaaSILAutorizzaImportFlussoRisposta, String, SilOutcome> result =
        service.authorizeIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          type,
          PaaSILAutorizzaImportFlussoRisposta::new,
          FaultBean::new,
          PaaSILAutorizzaImportFlussoRisposta::setFault,
          PaaSILAutorizzaImportFlussoRisposta::setRequestToken,
          PaaSILAutorizzaImportFlussoRisposta::setUploadUrl
        );

      assertNotNull(result);
      assertEquals(SilOutcome.KO, result.getRight());
      assertNotNull(result.getLeft());
      assertNull(result.getMiddle());
      assertNotNull(result.getLeft().getFault());
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

      Triple<PaaSILAutorizzaImportFlussoRisposta, String, SilOutcome> result =
        service.authorizeIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          type,
          PaaSILAutorizzaImportFlussoRisposta::new,
          FaultBean::new,
          PaaSILAutorizzaImportFlussoRisposta::setFault,
          PaaSILAutorizzaImportFlussoRisposta::setRequestToken,
          PaaSILAutorizzaImportFlussoRisposta::setUploadUrl
        );

      assertNotNull(result);
      assertEquals(SilOutcome.OK, result.getRight());
      assertNotNull(result.getLeft());
      assertEquals("456", result.getLeft().getRequestToken());
      assertEquals("http://upload.url", result.getLeft().getUploadUrl());
      assertNull(result.getMiddle());

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

      Triple<PaaSILAutorizzaImportFlussoRisposta, String, SilOutcome> result =
        service.authorizeIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          type,
          PaaSILAutorizzaImportFlussoRisposta::new,
          FaultBean::new,
          PaaSILAutorizzaImportFlussoRisposta::setFault,
          PaaSILAutorizzaImportFlussoRisposta::setRequestToken,
          PaaSILAutorizzaImportFlussoRisposta::setUploadUrl
        );

      assertNotNull(result);
      assertEquals(SilOutcome.OK, result.getRight());
      assertNotNull(result.getLeft());
      assertEquals("999", result.getLeft().getRequestToken());
      assertEquals("http://upload.null", result.getLeft().getUploadUrl());
      assertNull(result.getMiddle());

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

      Triple<PaaSILAutorizzaImportFlussoRisposta, String, SilOutcome> result =
        service.authorizeIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          type,
          PaaSILAutorizzaImportFlussoRisposta::new,
          FaultBean::new,
          PaaSILAutorizzaImportFlussoRisposta::setFault,
          PaaSILAutorizzaImportFlussoRisposta::setRequestToken,
          PaaSILAutorizzaImportFlussoRisposta::setUploadUrl
        );

      assertNotNull(result);
      assertEquals(SilOutcome.OK, result.getRight());
      assertNotNull(result.getLeft());
      assertEquals("654", result.getLeft().getRequestToken());
      assertNull(result.getLeft().getUploadUrl());
      assertNull(result.getMiddle());

      verify(ingestionFlowFileServiceMock).createIngestionFlowFileReservation(any(), eq(accessToken));
      verify(ingestionFlowFileReservationServiceMock).generateUploadUrl(any());
    }
  }

}
