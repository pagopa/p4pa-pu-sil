package it.gov.pagopa.pu.sil.service.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionFlowFileProcessingStatusServiceTest {
  @Mock
  IngestionFlowFileService ingestionFlowFileServiceMock;

  private IngestionFlowFileProcessingStatusService service;

  @BeforeEach
  void setUp() {
    service = new IngestionFlowFileProcessingStatusService(ingestionFlowFileServiceMock);
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(ingestionFlowFileServiceMock);
  }

  @Test
  void givenUserNotAdminWhenAuthorizeIngestionFlowFileThenKo() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFileTypeEnum type = IngestionFlowFileTypeEnum.DP_INSTALLMENTS;

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(false);
      assertThrows(UnsupportedOperationException.class, () ->
        service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, type)
      );
      verifyNoInteractions(ingestionFlowFileServiceMock);
    }
  }

  @Test
  void givenUserAdminWhenAuthorizeIngestionFlowFileThenOk() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFileTypeEnum type = IngestionFlowFileTypeEnum.DP_INSTALLMENTS;
    String expectedType = type.name();
    IngestionFlowFile expectedIngestionFlowFile = new IngestionFlowFile()
      .ingestionFlowFileType(IngestionFlowFile.IngestionFlowFileTypeEnum.valueOf(expectedType));

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken))
        .thenReturn(expectedIngestionFlowFile);

      IngestionFlowFileStatus result = service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, type);
      assertNotNull(result);
      assertEquals(expectedIngestionFlowFile.getStatus().name(), result.name());

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }

  @Test
  void givenTypeMismatchWhenGetProcessingStatusThenThrowException() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("user1");
    String orgIpaCode = "ORG1";
    String accessToken = "token";
    IngestionFlowFile expectedIngestionFlowFile = new IngestionFlowFile()
      .ingestionFlowFileType(IngestionFlowFile.IngestionFlowFileTypeEnum.DP_INSTALLMENTS);

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.isAdminRole(eq(orgIpaCode), eq(userInfo))).thenReturn(true);
      when(ingestionFlowFileServiceMock.getIngestionFlowFile(1L, accessToken))
        .thenReturn(expectedIngestionFlowFile);

      assertThrows(IllegalArgumentException.class, () ->
        service.getProcessingStatus(userInfo, accessToken, orgIpaCode, 1L, IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
      );

      verify(ingestionFlowFileServiceMock).getIngestionFlowFile(1L, accessToken);
    }
  }
}
