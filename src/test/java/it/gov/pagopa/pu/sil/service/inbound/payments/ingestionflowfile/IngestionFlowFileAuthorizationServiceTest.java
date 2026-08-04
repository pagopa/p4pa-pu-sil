package it.gov.pagopa.pu.sil.service.inbound.payments.ingestionflowfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.sil.connector.processexecutions.IngestionFlowFileService;
import it.gov.pagopa.pu.sil.dto.generated.ImportFileResponseDTO;
import it.gov.pagopa.pu.sil.enums.legacy.IngestionFlowFileLegacyType;
import it.gov.pagopa.pu.sil.exception.IngestionFlowFileTypeValidationException;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
    // Given
    String orgIpaCode = "ORG1";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", "OTHERIPACODE");
    userInfo.setUserId("user1");
    String accessToken = "token";
    IngestionFlowFileTypeEnum type = IngestionFlowFileTypeEnum.DP_INSTALLMENTS;

    // When, Then
    assertThrows(AuthorizationDeniedException.class, () ->
      service.authorizeIngestionFlowFile(
        userInfo,
        accessToken,
        orgIpaCode,
        type
      )
    );
  }

  @Test
  void givenUserIsAdminWhenAuthorizeIngestionFlowFileThenOk() {
    // Given
    String orgIpaCode = "ORG2";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    userInfo.setUserId("admin1");
    String accessToken = "token2";
    IngestionFlowFileTypeEnum type = IngestionFlowFileTypeEnum.DP_INSTALLMENTS;

    when(ingestionFlowFileServiceMock.createIngestionFlowFileReservation(any(), eq(accessToken))).thenReturn(456L);
    when(ingestionFlowFileReservationServiceMock.generateUploadUrl(any())).thenReturn("http://upload.url");

    // When
    ImportFileResponseDTO result = service.authorizeIngestionFlowFile(
      userInfo,
      accessToken,
      orgIpaCode,
      type
    );

    // Then
    assertNotNull(result);
    assertEquals("456", result.getImportId());
    assertEquals("http://upload.url", result.getUploadUrl());
  }

  @Test
  void givenUploadUrlNullWhenAuthorizeIngestionFlowFileThenOk() {
    // Given
    String orgIpaCode = "ORG4";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    userInfo.setUserId("admin2");
    String accessToken = "token4";
    IngestionFlowFileTypeEnum type = IngestionFlowFileTypeEnum.DP_INSTALLMENTS;

    when(ingestionFlowFileServiceMock.createIngestionFlowFileReservation(any(), eq(accessToken))).thenReturn(654L);
    when(ingestionFlowFileReservationServiceMock.generateUploadUrl(any())).thenReturn(null);

    // When
    ImportFileResponseDTO result =
      service.authorizeIngestionFlowFile(
        userInfo,
        accessToken,
        orgIpaCode,
        type
      );

    // Then
    assertNotNull(result);
    assertEquals("654", result.getImportId());
    assertNull(result.getUploadUrl());
  }

  @Test
  void givenUserIsAdminWhenAuthorizeTreasuryIngestionFlowFileThenOk() {
    //Given
    String orgIpaCode = "ORG2";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    userInfo.setUserId("admin1");
    String accessToken = "token2";

    when(ingestionFlowFileServiceMock.createIngestionFlowFileReservation(any(), eq(accessToken))).thenReturn(456L);
    when(ingestionFlowFileReservationServiceMock.generateUploadUrl(any())).thenReturn("http://upload.url");

    ImportFileResponseDTO result = service.authorizeTreasuryIngestionFlowFile(
      userInfo,
      accessToken,
      orgIpaCode,
      IngestionFlowFileLegacyType.TREASURY_OPI.getLegacyValue()
    );

    assertNotNull(result);
    assertEquals("456", result.getImportId());
    assertEquals("http://upload.url", result.getUploadUrl());
  }

  @Test
  void givenUserIsAdminAndWrongTypeWhenAuthorizeTreasuryIngestionFlowFileThenThrowsIngestionFlowFileTypeNotValidException() {
    // Given
    String orgIpaCode = "ORG2";
    UserInfo userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
    userInfo.setUserId("admin1");
    String accessToken = "token2";

    // When, Then
    assertThrows(IngestionFlowFileTypeValidationException.class,
      () -> service.authorizeTreasuryIngestionFlowFile(
        userInfo,
        accessToken,
        orgIpaCode,
        "A"
      ));
  }
}
