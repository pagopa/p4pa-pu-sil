package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.sil.dto.generated.ImportFileResponseDTO;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.RegistryLoggerTest;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileAuthorizationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MassiveImportControllerTest {
  @Mock
  private IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationServiceMock;
  @Mock
  private RegistryLogger registryLoggerMock;

  @InjectMocks
  private MassiveImportController controller;

  private UserInfo userInfo;

  @BeforeEach
  void setUp() {
    userInfo = new UserInfo();
    userInfo.setMappedExternalUserId("fakeExternalUser");
    Authentication authentication = new UsernamePasswordAuthenticationToken(userInfo, "fakeAccessToken");
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  void whenMassiveImportRequestThenOk() {
    // Given
    String orgFiscalCode = "ORG123456789";
    String accessToken = "fakeAccessToken";
    String userIpaCode = "userIpaCode";
    IngestionFlowFileTypeEnum dpInstallments = IngestionFlowFileTypeEnum.DP_INSTALLMENTS;
    ImportFileResponseDTO expectedResult = new ImportFileResponseDTO();

    try (MockedStatic<AuthorizationService> authMock = mockStatic(AuthorizationService.class)) {
      authMock.when(() -> AuthorizationService.getOrgIpaCodeFromUserInfo(userInfo, orgFiscalCode))
        .thenReturn(userIpaCode);
      RegistryContextData expectedContextData = RegistryContextData.builder()
        .orgFiscalCode(orgFiscalCode)
        .eventType(RegistryEventType.PTDP_paaSILAutorizzaImportFlusso)
        .loggedUser(userInfo)
        .build();
      RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, expectedContextData, dpInstallments, false, false);

      when(ingestionFlowFileAuthorizationServiceMock
        .authorizeIngestionFlowFile(userInfo, accessToken, userIpaCode, dpInstallments))
        .thenReturn(expectedResult);

      // When
      ResponseEntity<ImportFileResponseDTO> response = controller
        .massiveImportRequest(orgFiscalCode, dpInstallments);

      // Then
      Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
      Assertions.assertSame(expectedResult, response.getBody());
    }

  }
}
