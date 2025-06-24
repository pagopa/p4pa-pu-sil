package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.dto.PaymentsProcessingStatusDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.legacy.IngestionFlowFileLegacyStatus;
import it.gov.pagopa.pu.sil.exception.ExportFileClientException;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.RegistryLoggerTest;
import it.gov.pagopa.pu.sil.registry.extrainfo.RegistryExtraInfoHandlerPaaSILImportaDovuto;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.security.SecurityUtilsTest;
import it.gov.pagopa.pu.sil.service.exportfile.PaaSILPrenotaExportFlussoService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileAuthorizationService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileProcessingStatusService;
import it.gov.pagopa.pu.sil.service.paasillimportadovuto.PaaSILImportaDovutoService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.*;
import it.veneto.regione.pagamenti.ente.ppthead.IntestazionePPT;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.ws.soap.SoapHeaderElement;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class PuForOrganizationPaymentsEndpointTest {

  private static final String VALID_ORG_IPA_CODE = "IPA_2";
  public static final String VALID_ORGANIZATION_FISCAL_CODE = "CF_2";
  private static final String INVALID_ORG_IPA_CODE = "IPA_1";

  @Mock
  private RegistryLogger registryLoggerMock;
  @Mock
  private IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationServiceMock;
  @Mock
  private PaaSILImportaDovutoService paaSILImportaDovutoServiceMock;
  @Mock
  private RegistryExtraInfoHandlerPaaSILImportaDovuto registryExtraInfoHandlerPaaSILImportaDovutoServiceMock;
  @Mock
  private IngestionFlowFileProcessingStatusService ingestionFlowFileProcessingStatusServiceMock;
  @Mock
  private PaaSILPrenotaExportFlussoService paaSILPrenotaExportFlussoServiceMock;

  @InjectMocks
  private PuForOrganizationPaymentsEndpoint puForOrganizationPaymentsEndpoint;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();
  private final String accessToken = "ACCESSTOKEN";
  private UserInfo userInfo;

  @BeforeEach
  void init() {
    //set a valid user for org IPA_2
    userInfo = new UserInfo();
    userInfo.setMappedExternalUserId("USERID");
    userInfo.setOrganizations(List.of(
      new UserOrganizationRoles("OID1", 1L, INVALID_ORG_IPA_CODE, "CF_1", "email", List.of("")),
      new UserOrganizationRoles("OID2", 2L, VALID_ORG_IPA_CODE, VALID_ORGANIZATION_FISCAL_CODE, "email", List.of(SecurityUtils.OPERATOR_ROLE_ADMIN))
    ));
    SecurityUtilsTest.configureSecurityContext(accessToken, userInfo);
  }

  @AfterEach
  void clear(){
    RequestContextHolder.resetRequestAttributes();
    SecurityUtilsTest.clearSecurityContext();
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      registryLoggerMock,
      ingestionFlowFileAuthorizationServiceMock,
      paaSILImportaDovutoServiceMock,
      registryExtraInfoHandlerPaaSILImportaDovutoServiceMock,
      ingestionFlowFileProcessingStatusServiceMock
    );
  }

  private void configureRegistryLoggerMock(RegistryContextData contextData, Object request, boolean withExtraInfo) {
    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, contextData, request, withExtraInfo);
  }

  // region PaaSILChiediStatoImportFlusso

  @Test
  void givenValidRequestWhenPaaSILChiediStatoImportFlussoThenResponseContainsExpectedStatusAndUrl() throws Exception {
    // Given
    Long requestToken = 12345L;
    String expectedUrl = "https://upload.url";
    PaaSILChiediStatoImportFlusso request = podamFactory.manufacturePojo(PaaSILChiediStatoImportFlusso.class);
    request.setRequestToken(String.valueOf(requestToken));
    request.setFileAvvisi(Boolean.FALSE);
    request.setFileScarti(Boolean.TRUE);
    request.setFileIUV(Boolean.TRUE);
    PaymentsProcessingStatusDTO statusDTO = new PaymentsProcessingStatusDTO();
    statusDTO.setUrlNotice(null);
    statusDTO.setUrlImported(expectedUrl + "/imported");
    statusDTO.setUrlErrors(expectedUrl + "/errors");
    statusDTO.setStatus(IngestionFlowFileStatus.COMPLETED);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    Mockito.when(ingestionFlowFileProcessingStatusServiceMock.getProcessingStatus(
      Mockito.eq(request), Mockito.same(userInfo), Mockito.same(accessToken), Mockito.eq(VALID_ORG_IPA_CODE), Mockito.eq(requestToken), Mockito.eq(IngestionFlowFile.IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
    )).thenReturn(statusDTO);

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.paaSILChiediStatoImportFlusso)
      .orgFiscalCode(VALID_ORGANIZATION_FISCAL_CODE)
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request, false);

    // When
    PaaSILChiediStatoImportFlussoRisposta response =
      puForOrganizationPaymentsEndpoint.paaSILChiediStatoImportFlusso(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(IngestionFlowFileLegacyStatus.fromValue2LegacyValue(statusDTO.getStatus()), response.getStato());
    Assertions.assertEquals(statusDTO.getUrlImported(), response.getUrlFileIUV());
    Assertions.assertEquals(statusDTO.getUrlErrors(), response.getUrlFileScarti());
    Assertions.assertEquals(statusDTO.getUrlNotice(), response.getUrlFileAvvisi());
  }
  // endregion

  // region PaaSILAutorizzaImportFlusso
  @Test
  void givenValidRequestWhenPaaSILAutorizzaImportFlussoThenResponseContainsExpectedTokenAndUrl() throws Exception {
    // Given
    PaaSILAutorizzaImportFlusso request = podamFactory.manufacturePojo(PaaSILAutorizzaImportFlusso.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    Long expectedToken = 12345L;
    String expectedUrl = "https://upload.url";

    Mockito.when(ingestionFlowFileAuthorizationServiceMock.authorizeIngestionFlowFile(
      Mockito.same(userInfo), Mockito.same(accessToken), Mockito.eq(VALID_ORG_IPA_CODE), Mockito.eq(IngestionFlowFile.IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
    )).thenReturn(Pair.of(expectedToken, expectedUrl));

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.paaSILAutorizzaImportFlusso)
      .orgFiscalCode(VALID_ORGANIZATION_FISCAL_CODE)
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request, false);

    // When
    PaaSILAutorizzaImportFlussoRisposta response = puForOrganizationPaymentsEndpoint.paaSILAutorizzaImportFlusso(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(String.valueOf(expectedToken), response.getRequestToken());
    Assertions.assertEquals(expectedUrl, response.getUploadUrl());
  }

  // endregion

  // region PasSILImportaDovuto
  @Test
  void givenValidRequestWhenPaaSILImportaDovutoThenExtractRequestExtraInfoIsCalled() throws Exception {
    // Given
    PaaSILImportaDovuto request = podamFactory.manufacturePojo(PaaSILImportaDovuto.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    Triple<PaaSILImportaDovutoRisposta, String, RegistryOutcome> tripleResponse = Triple.of(
      new PaaSILImportaDovutoRisposta(),
      "iuv",
      RegistryOutcome.OK
    );

    Mockito.when(paaSILImportaDovutoServiceMock.paaSILImportaDovuto(Mockito.same(userInfo), Mockito.eq(VALID_ORG_IPA_CODE), Mockito.same(request)))
      .thenReturn(tripleResponse);

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.paaSILImportaDovuto)
      .orgFiscalCode(VALID_ORGANIZATION_FISCAL_CODE)
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request, true);

    // When
    PaaSILImportaDovutoRisposta result = puForOrganizationPaymentsEndpoint.paaSILImportaDovuto(request, header);

    // Then
    Assertions.assertNotNull(result);
    Mockito.verify(registryExtraInfoHandlerPaaSILImportaDovutoServiceMock).extractRequestExtraInfo(request, header);
    Mockito.verify(registryExtraInfoHandlerPaaSILImportaDovutoServiceMock).extractResponseExtraInfo(result);
  }

  // endregion

  // region PaaSILChiediAvvisiPendenti
  @Test
  void givenAnyWhenPaaSILChiediAvvisiPendentiThenFault() throws Exception {
    testFaultResponse(PaaSILChiediAvvisiPendenti.class,
      SilFaults.PAA_SYSTEM_ERROR.code(),
      puForOrganizationPaymentsEndpoint::paaSILChiediAvvisiPendenti);
  }
  // endregion

  // region PaaSILChiediPosizioniAperte
  @Test
  void givenAnyWhenPaaSILChiediPosizioniAperteThenFault() throws Exception {
    testFaultResponse(PaaSILChiediPosizioniAperte.class,
      SilFaults.PAA_SYSTEM_ERROR.code(),
      puForOrganizationPaymentsEndpoint::paaSILChiediPosizioniAperte);
  }
  // endregion

  // region PaaSILChiediPosizioniChiuse
  @Test
  void givenAnyWhenPaaSILRegistraPagamentoThenFault() throws Exception {
    testFaultResponse(PaaSILRegistraPagamento.class,
      SilFaults.PAA_SYSTEM_ERROR.code(),
      puForOrganizationPaymentsEndpoint::paaSILRegistraPagamento);
  }
  // endregion

  // region PaaSILPrenotaExportFlusso
  @Test
  void givenValidRequestWhenPaaSILPrenotaExportFlussoThenResponseContainsExpectedToken() throws Exception {
    // Given
    PaaSILPrenotaExportFlusso request = podamFactory.manufacturePojo(PaaSILPrenotaExportFlusso.class);
    request.setIdentificativoTipoDovuto("1");
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    Long expectedToken = 12345L;

    Mockito.when(paaSILPrenotaExportFlussoServiceMock.paaSILPrenotaExportFlusso(
      Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
    )).thenReturn(expectedToken);

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.paaSILPrenotaExportFlusso)
      .orgFiscalCode(VALID_ORGANIZATION_FISCAL_CODE)
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request, false);

    // When
    PaaSILPrenotaExportFlussoRisposta response = puForOrganizationPaymentsEndpoint.paaSILPrenotaExportFlusso(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(String.valueOf(expectedToken), response.getRequestToken());
  }

  @Test
  void givenValidRequestWhenPaaSILPrenotaExportFlussoThrowsClientExceptionThenResponseContainsExpectedFaultCode() throws Exception {
    // Given
    PaaSILPrenotaExportFlusso request = podamFactory.manufacturePojo(PaaSILPrenotaExportFlusso.class);
    request.setIdentificativoTipoDovuto("1");
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    Mockito.when(paaSILPrenotaExportFlussoServiceMock.paaSILPrenotaExportFlusso(
      Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
    )).thenThrow(new ExportFileClientException(ProcessExecutionsErrorDTO.CodeEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE, "Invalid time range"));

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.paaSILPrenotaExportFlusso)
      .orgFiscalCode(VALID_ORGANIZATION_FISCAL_CODE)
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request, false);

    // When
    PaaSILPrenotaExportFlussoRisposta response = puForOrganizationPaymentsEndpoint.paaSILPrenotaExportFlusso(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SilFaults.PAA_INTERVALLO_DATE_NON_VALIDO.code(), response.getFault().getFaultCode());
  }

  @Test
  void givenValidRequestWhenPaaSILPrenotaExportFlussoThrowsServiceExceptionThenResponseContainsExpectedFaultCode() throws Exception {
    // Given
    PaaSILPrenotaExportFlusso request = podamFactory.manufacturePojo(PaaSILPrenotaExportFlusso.class);
    request.setIdentificativoTipoDovuto("1");
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    Mockito.when(paaSILPrenotaExportFlussoServiceMock.paaSILPrenotaExportFlusso(
      Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
    )).thenThrow(new ExportFileServiceException(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, "Identificativo tipo dovuto non valido"));

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.paaSILPrenotaExportFlusso)
      .orgFiscalCode(VALID_ORGANIZATION_FISCAL_CODE)
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request, false);

    // When
    PaaSILPrenotaExportFlussoRisposta response = puForOrganizationPaymentsEndpoint.paaSILPrenotaExportFlusso(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO.code(), response.getFault().getFaultCode());
  }
  // endregion

  private interface TestFunction<T, R> {
    R apply(T request, SoapHeaderElement header) throws Exception;
  }

  private <T, R extends Risposta> void testFaultResponse(Class<T> requestClass,
                                                         String faultCode,
                                                         TestFunction<T, R> testFunction) throws Exception {
    T request = podamFactory.manufacturePojo(requestClass);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    //set IPA_2 because the test user is authorized for this org
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    R response = testFunction.apply(request, header);

    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(faultCode, response.getFault().getFaultCode());
  }
}
