package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO.CodeEnum;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.legacy.IngestionFlowFileLegacyStatus;
import it.gov.pagopa.pu.sil.exception.ExportFileClientException;
import it.gov.pagopa.pu.sil.exception.IngestionFlowFileTypeValidationException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.RegistryLoggerTest;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.security.SecurityUtilsTest;
import it.gov.pagopa.pu.sil.service.exportfile.PivotSILPrenotaExportFlussoRiconciliazioneService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileAuthorizationService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileProcessingStatusService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.pivot.ente.*;
import it.veneto.regione.pagamenti.pivot.ente.ppthead.IntestazionePPT;
import org.apache.commons.lang3.tuple.Pair;
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

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class PuForOrganizationReconciliationEndpointTest {

  private static final String VALID_ORG_IPA_CODE = "IPA_2";
  public static final String VALID_ORGANIZATION_FISCAL_CODE = "CF_2";
  private static final String INVALID_ORG_IPA_CODE = "IPA_1";
  public static final String INVALID_ORGANIZATION_FISCAL_CODE = "CF_1";

  @Mock
  private RegistryLogger registryLoggerMock;
  @Mock
  private IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationServiceMock;
  @Mock
  private IngestionFlowFileProcessingStatusService ingestionFlowFileProcessingStatusServiceMock;
  @Mock
  private PivotSILPrenotaExportFlussoRiconciliazioneService pivotSILPrenotaExportFlussoRiconciliazioneServiceMock;

  @InjectMocks
  private PuForOrganizationReconciliationEndpoint puForOrganizationReconciliationEndpoint;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();
  private final String accessToken = "ACCESSTOKEN";
  private UserInfo userInfo;

  @BeforeEach
  void init() {
    //set a valid user for org IPA_2
    userInfo = new UserInfo();
    userInfo.setMappedExternalUserId("USERID");
    userInfo.setOrganizations(List.of(
      new UserOrganizationRoles("OID1", 1L, INVALID_ORG_IPA_CODE, INVALID_ORGANIZATION_FISCAL_CODE, "email", List.of("")),
      new UserOrganizationRoles("OID2", 2L, VALID_ORG_IPA_CODE, VALID_ORGANIZATION_FISCAL_CODE, "email", List.of(SecurityUtils.OPERATOR_ROLE_ADMIN))
    ));
    SecurityUtilsTest.configureSecurityContext(accessToken, userInfo);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      registryLoggerMock,
      ingestionFlowFileAuthorizationServiceMock,
      ingestionFlowFileProcessingStatusServiceMock,
      pivotSILPrenotaExportFlussoRiconciliazioneServiceMock);
  }

  @AfterEach
  void clear(){
    RequestContextHolder.resetRequestAttributes();
    SecurityUtilsTest.clearSecurityContext();
  }

  private void configureRegistryLoggerMock(RegistryContextData contextData, Object request) {
    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, contextData, request, false);
  }

  //region pivotSILChiediStatoImportFlussoTesoreria

  @Test
  void givenValidRequestWhenPivotSILChiediStatoImportFlussoTesoreriaThenResponseContainsExpectedStatus() throws Exception {
    //  Given
    Long requestToken = 12345L;
    PivotSILChiediStatoImportFlussoTesoreria request = podamFactory.manufacturePojo(PivotSILChiediStatoImportFlussoTesoreria.class);
    request.setRequestToken(String.valueOf(requestToken));
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    IngestionFlowFile ingestionFlowFile = podamFactory.manufacturePojo(IngestionFlowFile.class)
      .status(IngestionFlowFileStatus.COMPLETED);

    IngestionFlowFileTypeEnum[] ingestionFlowFileTypeEnums = {IngestionFlowFileTypeEnum.TREASURY_OPI,
      IngestionFlowFileTypeEnum.TREASURY_CSV,
      IngestionFlowFileTypeEnum.TREASURY_XLS,
      IngestionFlowFileTypeEnum.TREASURY_POSTE};

    Mockito.when(ingestionFlowFileProcessingStatusServiceMock.getIngestionFlowFile(
      Mockito.same(userInfo), Mockito.same(accessToken), Mockito.eq(VALID_ORG_IPA_CODE), Mockito.eq(requestToken), Mockito.eq(ingestionFlowFileTypeEnums)
    )).thenReturn(ingestionFlowFile);

    // When
    PivotSILChiediStatoImportFlussoTesoreriaRisposta response =
      puForOrganizationReconciliationEndpoint.pivotSILChiediStatoImportFlussoTesoreria(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(IngestionFlowFileLegacyStatus.fromValue2LegacyValue(ingestionFlowFile.getStatus()), response.getStato());
  }
  //endregion

  //region pivotSILChiediStatoImportFlusso

  @Test
  void givenValidRequestWhenPivotSILChiediStatoImportFlussoThenResponseContainsExpectedStatus() throws Exception {
    // Given
    Long requestToken = 12345L;
    PivotSILChiediStatoImportFlusso request = podamFactory.manufacturePojo(PivotSILChiediStatoImportFlusso.class);
    request.setRequestToken(String.valueOf(requestToken));
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    IngestionFlowFile ingestionFlowFile = podamFactory.manufacturePojo(IngestionFlowFile.class)
      .status(IngestionFlowFileStatus.COMPLETED);

    Mockito.when(ingestionFlowFileProcessingStatusServiceMock.getIngestionFlowFile(
      Mockito.same(userInfo), Mockito.same(accessToken), Mockito.eq(VALID_ORG_IPA_CODE), Mockito.eq(requestToken), Mockito.eq(IngestionFlowFileTypeEnum.PAYMENT_NOTIFICATION)
    )).thenReturn(ingestionFlowFile);

    // When
    PivotSILChiediStatoImportFlussoRisposta response =
      puForOrganizationReconciliationEndpoint.pivotSILChiediStatoImportFlusso(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(IngestionFlowFileLegacyStatus.fromValue2LegacyValue(ingestionFlowFile.getStatus()), response.getStato());
  }

  //endregion

  //region pivotSILAutorizzaImportFlusso
  @Test
  void givenValidRequestWhenPivotSILAutorizzaImportFlussoThenResponseContainsExpectedTokenAndUrl() throws Exception {
    // Given
    PivotSILAutorizzaImportFlusso request = podamFactory.manufacturePojo(PivotSILAutorizzaImportFlusso.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    Long expectedToken = 98765L;
    String expectedUrl = "https://upload.pivot.url";

    Mockito.when(ingestionFlowFileAuthorizationServiceMock.authorizeIngestionFlowFile(
      Mockito.same(userInfo), Mockito.same(accessToken), Mockito.eq(VALID_ORG_IPA_CODE), Mockito.eq(IngestionFlowFileTypeEnum.PAYMENT_NOTIFICATION)
    )).thenReturn(Pair.of(expectedToken, expectedUrl));

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.pivotSILAutorizzaImportFlusso)
      .orgFiscalCode(VALID_ORGANIZATION_FISCAL_CODE)
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request);

    // When
    PivotSILAutorizzaImportFlussoRisposta response =
      puForOrganizationReconciliationEndpoint.pivotSILAutorizzaImportFlusso(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(String.valueOf(expectedToken), response.getRequestToken());
    Assertions.assertEquals(expectedUrl, response.getUploadUrl());
  }

  //endregion

  //region pivotSILAutorizzaImportFlussoTesoreria

  @Test
  void givenValidRequestWhenPivotSILAutorizzaImportFlussoTesoreriaThenResponseContainsExpectedTokenAndUrl() throws Exception {
    // Given
    PivotSILAutorizzaImportFlussoTesoreria request = podamFactory.manufacturePojo(PivotSILAutorizzaImportFlussoTesoreria.class);
    request.setTipoFlusso(IngestionFlowFileTypeEnum.TREASURY_OPI.name());
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    Long expectedToken = 98765L;
    String expectedUrl = "https://upload.pivot.url";

    Mockito.when(ingestionFlowFileAuthorizationServiceMock.authorizeTreasuryIngestionFlowFile(
      Mockito.same(userInfo), Mockito.same(accessToken), Mockito.eq(VALID_ORG_IPA_CODE), Mockito.eq(request.getTipoFlusso())
    )).thenReturn(Pair.of(expectedToken, expectedUrl));

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.pivotSILAutorizzaImportFlussoTesoreria)
      .orgFiscalCode(VALID_ORGANIZATION_FISCAL_CODE)
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request);

    // When
    PivotSILAutorizzaImportFlussoTesoreriaRisposta response =
      puForOrganizationReconciliationEndpoint.pivotSILAutorizzaImportFlussoTesoreria(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(String.valueOf(expectedToken), response.getRequestToken());
    Assertions.assertEquals(expectedUrl, response.getUploadUrl());
  }

  @Test
  void givenIngestionFlowFileTypeValidationExceptionWhenPivotSILAutorizzaImportFlussoTesoreriaThenCustomHandlerIsUsed() throws Exception {
    // Given
    PivotSILAutorizzaImportFlussoTesoreria request = podamFactory.manufacturePojo(PivotSILAutorizzaImportFlussoTesoreria.class);
    request.setTipoFlusso(IngestionFlowFileTypeEnum.DP_INSTALLMENTS.name());
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    String customMessage = "Tipo flusso non valido";

    Mockito.when(ingestionFlowFileAuthorizationServiceMock.authorizeTreasuryIngestionFlowFile(
      Mockito.same(userInfo), Mockito.same(accessToken), Mockito.eq(VALID_ORG_IPA_CODE), Mockito.eq(request.getTipoFlusso())
    )).thenThrow(new IngestionFlowFileTypeValidationException(customMessage));

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.pivotSILAutorizzaImportFlussoTesoreria)
      .orgFiscalCode(VALID_ORGANIZATION_FISCAL_CODE)
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request);

    // When
    PivotSILAutorizzaImportFlussoTesoreriaRisposta response =
      puForOrganizationReconciliationEndpoint.pivotSILAutorizzaImportFlussoTesoreria(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PIVOT_TIPO_FLUSSO_NON_VALIDO.code(), response.getFault().getFaultCode());
    Assertions.assertTrue(response.getFault().getDescription().contains(customMessage));
  }

  @Test
  void givenGenericExceptionWhenPivotSILAutorizzaImportFlussoTesoreriaThenBaseHandlerIsUsed() throws Exception {
    // Given
    PivotSILAutorizzaImportFlussoTesoreria request = podamFactory.manufacturePojo(PivotSILAutorizzaImportFlussoTesoreria.class);
    request.setTipoFlusso(IngestionFlowFileTypeEnum.TREASURY_OPI.name());
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(INVALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    Mockito.when(ingestionFlowFileAuthorizationServiceMock.authorizeTreasuryIngestionFlowFile(
      Mockito.same(userInfo), Mockito.same(accessToken), Mockito.eq(INVALID_ORG_IPA_CODE), Mockito.eq(request.getTipoFlusso())
    )).thenThrow(new UnauthorizedException("Utente non autorizzato"));

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.pivotSILAutorizzaImportFlussoTesoreria)
      .orgFiscalCode(INVALID_ORGANIZATION_FISCAL_CODE)
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request);

    // When
    PivotSILAutorizzaImportFlussoTesoreriaRisposta response =
      puForOrganizationReconciliationEndpoint.pivotSILAutorizzaImportFlussoTesoreria(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PIVOT_ENTE_NON_VALIDO.code(), response.getFault().getFaultCode());
  }

  // endregion

  //region pivotSILPrenotaExportFlussoRiconciliazione
  @Test
  void givenValidRequestWhenPivotSILPrenotaExportFlussoRiconciliazioneThenResponseContainsExpectedTokenAndDateTo() throws Exception {
    // Given
    GregorianCalendar fromCal = new GregorianCalendar(2023, 0, 1); // Jan 1, 2023
    GregorianCalendar toCal = new GregorianCalendar(2023, 11, 31); // Dec 31, 2023
    XMLGregorianCalendar fromXml = DatatypeFactory.newInstance().newXMLGregorianCalendar(fromCal);
    XMLGregorianCalendar toXml = DatatypeFactory.newInstance().newXMLGregorianCalendar(toCal);
    PivotSILPrenotaExportFlussoRiconciliazione request = podamFactory.manufacturePojo(PivotSILPrenotaExportFlussoRiconciliazione.class);
    request.setDataUltimoAggiornamentoDa(fromXml);
    request.setDataUltimoAggiornamentoA(toXml);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    Long expectedToken = 12345L;
    PivotSILPrenotaExportFlussoRiconciliazioneRisposta expectedResponse = new PivotSILPrenotaExportFlussoRiconciliazioneRisposta();
    expectedResponse.setRequestToken(String.valueOf(expectedToken));
    expectedResponse.setDataA(request.getDataUltimoAggiornamentoA());

    Mockito.when(pivotSILPrenotaExportFlussoRiconciliazioneServiceMock.doReservation(
      Mockito.same(userInfo), Mockito.same(accessToken), Mockito.eq(VALID_ORG_IPA_CODE), Mockito.eq(request)
    )).thenReturn(expectedResponse);

    // When
    PivotSILPrenotaExportFlussoRiconciliazioneRisposta response =
      puForOrganizationReconciliationEndpoint.pivotSILPrenotaExportFlussoRiconciliazione(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(String.valueOf(expectedToken), response.getRequestToken());
    Assertions.assertEquals(toXml, response.getDataA());
  }

  @Test
  void givenInvalidFileVersionWhenPivotSILPrenotaExportFlussoRiconciliazioneThenFault() throws Exception {
    PivotSILPrenotaExportFlussoRiconciliazione request = podamFactory.manufacturePojo(PivotSILPrenotaExportFlussoRiconciliazione.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    Mockito.when(pivotSILPrenotaExportFlussoRiconciliazioneServiceMock
        .doReservation(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
      .thenThrow(new ExportFileClientException(
        CodeEnum.PROCESS_EXECUTIONS_INVALID_FILE_VERSION, "Invalid file version"));

    PivotSILPrenotaExportFlussoRiconciliazioneRisposta response =
      puForOrganizationReconciliationEndpoint.pivotSILPrenotaExportFlussoRiconciliazione(request, header);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PIVOT_VERSIONE_TRACCIATO_NON_VALIDA.code(), response.getFault().getFaultCode());
  }

  @Test
  void givenInvalidTimeRangeWhenPivotSILPrenotaExportFlussoRiconciliazioneThenFault() throws Exception {
    PivotSILPrenotaExportFlussoRiconciliazione request = podamFactory.manufacturePojo(PivotSILPrenotaExportFlussoRiconciliazione.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    Mockito.when(pivotSILPrenotaExportFlussoRiconciliazioneServiceMock
        .doReservation(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
      .thenThrow(new ExportFileClientException(
        CodeEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE, "Invalid time range"));

    PivotSILPrenotaExportFlussoRiconciliazioneRisposta response =
      puForOrganizationReconciliationEndpoint.pivotSILPrenotaExportFlussoRiconciliazione(request, header);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PIVOT_INTERVALLO_DATE_NON_VALIDO.code(), response.getFault().getFaultCode());
  }

  @Test
  void givenGenericExceptionWhenPivotSILPrenotaExportFlussoRiconciliazioneThenSystemErrorFault() throws Exception {
    PivotSILPrenotaExportFlussoRiconciliazione request = podamFactory.manufacturePojo(PivotSILPrenotaExportFlussoRiconciliazione.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    Mockito.when(pivotSILPrenotaExportFlussoRiconciliazioneServiceMock
      .doReservation(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())
    ).thenThrow(new RuntimeException("Unexpected error"));

    PivotSILPrenotaExportFlussoRiconciliazioneRisposta response =
      puForOrganizationReconciliationEndpoint.pivotSILPrenotaExportFlussoRiconciliazione(request, header);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PIVOT_SYSTEM_ERROR.code(), response.getFault().getFaultCode());
  }
  // endregion

  //region pivotSILChiediPagatiRiconciliati
  @Test
  void givenAnyWhenPivotSILChiediPagatiRiconciliatiThenFault() throws Exception {
    testFaultResponse(PivotSILChiediPagatiRiconciliati.class,
      SilFaults.PIVOT_SYSTEM_ERROR.code(),
      puForOrganizationReconciliationEndpoint::pivotSILChiediPagatiRiconciliati);
  }
  //endregion

  //region pivotSILAutorizzaImportFlussoRendicontazione
  @Test
  void givenAnyWhenPivotSILAutorizzaImportFlussoRendicontazioneThenFault() throws Exception {
    testFaultResponse(PivotSILAutorizzaImportFlussoRendicontazione.class,
      SilFaults.PIVOT_SYSTEM_ERROR.code(),
      puForOrganizationReconciliationEndpoint::pivotSILAutorizzaImportFlussoRendicontazione);
  }
  //endregion

  //region pivotSILAutorizzaImportFlussoRT
  @Test
  void givenAnyWhenPivotSILAutorizzaImportFlussoRTThenFault() throws Exception {
    testFaultResponse(PivotSILAutorizzaImportFlussoRT.class,
      SilFaults.PIVOT_SYSTEM_ERROR.code(),
      puForOrganizationReconciliationEndpoint::pivotSILAutorizzaImportFlussoRT);
  }

  //endregion

  private interface TestFunction<T, R> {
    R apply(T request, SoapHeaderElement header) throws Exception;
  }

  private <T, R extends Risposta> void testFaultResponse(Class<T> requestClass,
                                                         String faultCode,
                                                         TestFunction<T, R> testFunction) throws Exception {
    T request = podamFactory.manufacturePojo(requestClass);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    R response = testFunction.apply(request, header);

    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(faultCode, response.getFault().getFaultCode());
  }
}
