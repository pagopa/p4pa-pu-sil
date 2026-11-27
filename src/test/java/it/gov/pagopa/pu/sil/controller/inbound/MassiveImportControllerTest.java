package it.gov.pagopa.pu.sil.controller.inbound;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.sil.dto.generated.ImportFileRequestDTO;
import it.gov.pagopa.pu.sil.dto.generated.ImportFileResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.ImportStatusResponseDTO;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.RegistryLoggerTest;
import it.gov.pagopa.pu.sil.security.SecurityUtilsTest;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.inbound.payments.ingestionflowfile.IngestionFlowFileAuthorizationService;
import it.gov.pagopa.pu.sil.service.inbound.payments.ingestionflowfile.IngestionFlowFileProcessingStatusService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MassiveImportControllerTest {
  @Mock
  private IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationServiceMock;
  @Mock
  private IngestionFlowFileProcessingStatusService ingestionFlowFileProcessingStatusServiceMock;
  @Mock
  private RegistryLogger registryLoggerMock;

  @InjectMocks
  private MassiveImportController controller;

  private final String accessToken = "fakeAccessToken";
  private final String orgFiscalCode = "ORG123456789";
  private final String orgIpaCode = "userIpaCode";
  private UserInfo userInfo;

  @BeforeEach
  void setUp() {
    userInfo = AuthorizationServiceTest.buildAdminUser(1L, orgFiscalCode, orgIpaCode);
    userInfo.setMappedExternalUserId("fakeExternalUser");
    SecurityUtilsTest.configureSecurityContext(accessToken, userInfo);
  }

  @AfterEach
  void clear(){
    SecurityUtilsTest.clearSecurityContext();
  }

  @Test
  void whenMassiveImportRequestThenOk() {
    // Given
    IngestionFlowFileTypeEnum dpInstallments = IngestionFlowFileTypeEnum.DP_INSTALLMENTS;
    ImportFileResponseDTO expectedResult = new ImportFileResponseDTO();

    RegistryContextData expectedContextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.PTDP_paaSILAutorizzaImportFlusso)
      .loggedUser(userInfo)
      .build();
    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, expectedContextData, dpInstallments, false, false);

    when(ingestionFlowFileAuthorizationServiceMock
      .authorizeIngestionFlowFile(userInfo, accessToken, orgIpaCode, dpInstallments))
      .thenReturn(expectedResult);

    // When
    ResponseEntity<ImportFileResponseDTO> response = controller
      .massiveImportRequest(orgFiscalCode, new ImportFileRequestDTO().importFileType(dpInstallments));

    // Then
    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertSame(expectedResult, response.getBody());

  }

  @Test
  void whenMassiveImportStatusThenOk() {
    // Given
    Long importId = 123L;
    ImportStatusResponseDTO expectedResult = new ImportStatusResponseDTO();

    when(ingestionFlowFileProcessingStatusServiceMock
      .getProcessingStatus(userInfo, accessToken, orgIpaCode, importId))
      .thenReturn(expectedResult);

    // When
    ResponseEntity<ImportStatusResponseDTO> response = controller
      .massiveImportStatus(orgFiscalCode, importId);

    // Then
    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertSame(expectedResult, response.getBody());
  }
}
