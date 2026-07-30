package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;
import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.dto.generated.DownloadUrl;
import it.gov.pagopa.pu.sil.dto.generated.ExportStatusResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.ImportFileResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.ImportStatusResponseDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.legacy.ExportFileLegacyStatus;
import it.gov.pagopa.pu.sil.enums.legacy.IngestionFlowFileLegacyStatus;
import it.gov.pagopa.pu.sil.exception.ExportFileClientException;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.exception.InvalidValueException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.RegistryLoggerTest;
import it.gov.pagopa.pu.sil.registry.extrainfo.RegistryExtraInfoHandlerPaaSILImportaDovuto;
import it.gov.pagopa.pu.sil.registry.extrainfo.RegistryExtraInfoHandlerPaaSILInviaCarrelloDovuti;
import it.gov.pagopa.pu.sil.registry.extrainfo.RegistryExtraInfoHandlerPaaSILInviaDovuti;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.security.SecurityUtilsTest;
import it.gov.pagopa.pu.sil.service.exportfile.ExportFileProcessingStatusService;
import it.gov.pagopa.pu.sil.service.exportfile.PaaSILPrenotaExportFlussoIncrementaleConRicevutaService;
import it.gov.pagopa.pu.sil.service.exportfile.PaaSILPrenotaExportFlussoService;
import it.gov.pagopa.pu.sil.service.immediatepayments.PaaSILInviaCarrelloDovutiService;
import it.gov.pagopa.pu.sil.service.immediatepayments.PaaSILInviaDovutiService;
import it.gov.pagopa.pu.sil.service.immediatepayments.PaaSILVerificaAvvisoService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileAuthorizationService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileProcessingStatusService;
import it.gov.pagopa.pu.sil.service.querypayments.*;
import it.gov.pagopa.pu.sil.service.singleimport.PaaSILImportaDovutoService;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.ws.soap.SoapHeaderElement;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;
import java.util.stream.Stream;

import static it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus.COMPLETED;
import static it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus.PROCESSING;
import static it.gov.pagopa.pu.sil.dto.generated.DownloadUrl.CodeEnum.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PuForOrganizationPaymentsEndpointTest {

  private static final String VALID_ORG_IPA_CODE = "IPA_2";
  public static final String VALID_ORGANIZATION_FISCAL_CODE = "CF_2";
  private static final String INVALID_ORG_IPA_CODE = "IPA_1";

  private static final String HARDCODED_AUTHORIZATION_TOKEN = "AUTHORIZATIONTOKEN";
  private static final String HARDCODED_IMPORT_PATH = "/IMPORTPATH";

  @Mock
  private RegistryLogger registryLoggerMock;
  @Mock
  private IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationServiceMock;
  @Mock
  private PaaSILImportaDovutoService paaSILImportaDovutoServiceMock;
  @Mock
  private PaaSILInviaDovutiService paaSILInviaDovutiServiceMock;
  @Mock
  private PaaSILInviaCarrelloDovutiService paaSILInviaCarrelloDovutiServiceMock;
  @Mock
  private PaaSILVerificaAvvisoService paaSILVerificaAvvisoServiceMock;
  @Mock
  private RegistryExtraInfoHandlerPaaSILImportaDovuto registryExtraInfoHandlerPaaSILImportaDovutoServiceMock;
  @Mock
  private RegistryExtraInfoHandlerPaaSILInviaDovuti registryExtraInfoHandlerPaaSILInviaDovutiMock;
  @Mock
  private RegistryExtraInfoHandlerPaaSILInviaCarrelloDovuti registryExtraInfoHandlerPaaSILInviaCarrelloDovutiMock;
  @Mock
  private IngestionFlowFileProcessingStatusService ingestionFlowFileProcessingStatusServiceMock;
  @Mock
  private PaaSILPrenotaExportFlussoService paaSILPrenotaExportFlussoServiceMock;
  @Mock
  private PaaSILPrenotaExportFlussoIncrementaleConRicevutaService paaSILPrenotaExportFlussoIncrementaleConRicevutaServiceMock;
  @Mock
  private PaaSILChiediPagatiService paaSILChiediPagatiServiceMock;
  @Mock
  private PaaSILChiediPagatiConRicevutaService paaSILChiediPagatiConRicevutaServiceMock;
  @Mock
  private PaaSILChiediEsitoCarrelloDovutiService paaSILChiediEsitoCarrelloDovutiServiceMock;
  @Mock
  private PaaSILChiediPosizioniAperteService paaSILChiediPosizioniAperteServiceMock;
  @Mock
  private PaaSILChiediStoricoPagamentiService paaSILChiediStoricoPagamentiServiceMock;
  @Mock
  private ExportFileProcessingStatusService exportFileProcessingStatusServiceMock;

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
  void afterEach() {
    clear();
    verifyNoMoreInteractions();
  }

  void clear(){
    RequestContextHolder.resetRequestAttributes();
    SecurityUtilsTest.clearSecurityContext();
  }

  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      registryLoggerMock,
      ingestionFlowFileAuthorizationServiceMock,
      paaSILImportaDovutoServiceMock,
      registryExtraInfoHandlerPaaSILImportaDovutoServiceMock,
      ingestionFlowFileProcessingStatusServiceMock,
      paaSILPrenotaExportFlussoServiceMock,
      paaSILPrenotaExportFlussoIncrementaleConRicevutaServiceMock,
      exportFileProcessingStatusServiceMock
    );
  }

  private void configureRegistryLoggerMock(RegistryContextData contextData, Object request, boolean withExtraInfo) {
    RegistryLoggerTest.configureRegistryLoggerMock(registryLoggerMock, contextData, request, withExtraInfo, withExtraInfo);
  }

  // region PaaSILChiediStatoExportFlusso
  @Test
  void givenValidRequestWhenPaaSILChiediStatoExportFlussoThenResponseContainsExpectedStatusAndUrl() throws Exception {
    // Given
    Long requestToken = 12345L;
    String expectedUrl = "https://export.url";
    PaaSILChiediStatoExportFlusso request = podamFactory.manufacturePojo(PaaSILChiediStatoExportFlusso.class);
    request.setRequestToken(String.valueOf(requestToken));

    Pair<ExportStatusResponseDTO.StatusEnum, String> processingStatus = Pair.of(
      ExportStatusResponseDTO.StatusEnum.COMPLETED,
      expectedUrl + "/exported"
    );
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    when(exportFileProcessingStatusServiceMock.getProcessingStatus(
      Mockito.same(userInfo), Mockito.same(accessToken), Mockito.eq(VALID_ORG_IPA_CODE), Mockito.eq(requestToken), Mockito.eq(ExportFile.ExportFileTypeEnum.PAID)
    )).thenReturn(processingStatus);

    // When
    PaaSILChiediStatoExportFlussoRisposta response =
      puForOrganizationPaymentsEndpoint.paaSILChiediStatoExportFlusso(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(ExportFileLegacyStatus.fromValue2LegacyValue(processingStatus.getLeft()), response.getStato());
    Assertions.assertEquals(processingStatus.getRight(), response.getDownloadUrl());
  }

  @Test
  void givenGenericExceptionWhenPivotSILChiediStatoExportFlussoThenSystemErrorFault() throws Exception {
    Long requestToken = 12345L;

    PaaSILChiediStatoExportFlusso request = podamFactory.manufacturePojo(PaaSILChiediStatoExportFlusso.class);
    request.setRequestToken(String.valueOf(requestToken));
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    when(exportFileProcessingStatusServiceMock.getProcessingStatus(
      Mockito.same(userInfo), Mockito.same(accessToken), Mockito.eq(VALID_ORG_IPA_CODE), Mockito.eq(requestToken), Mockito.eq(ExportFile.ExportFileTypeEnum.PAID)
    )).thenThrow(new RuntimeException("Unexpected error"));

    // When
    PaaSILChiediStatoExportFlussoRisposta response =
      puForOrganizationPaymentsEndpoint.paaSILChiediStatoExportFlusso(request, header);

    // Then
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PAA_SYSTEM_ERROR.code(), response.getFault().getFaultCode());
  }
  // endregion

  // region PaaSILChiediStatoImportFlusso

  @ParameterizedTest
  @MethodSource("paaSILChiediStatoImportFlussoProvider")
  void givenValidRequestWhenPaaSILChiediStatoImportFlussoThenResponseContainsExpectedStatusAndUrl(boolean flagImported,
                                                                                                  boolean flagError,
                                                                                                  boolean flagNotice,
                                                                                                  IngestionFlowFileStatus status,
                                                                                                  List<DownloadUrl> downloadUrls,
                                                                                                  String expectedImportedUrl,
                                                                                                  String expectedErrorUrl,
                                                                                                  String expectedNoticeUrl) throws Exception {
    // Given
    Long requestToken = 12345L;
    PaaSILChiediStatoImportFlusso request = podamFactory.manufacturePojo(PaaSILChiediStatoImportFlusso.class);
    request.setRequestToken(String.valueOf(requestToken));
    request.setFileAvvisi(flagNotice);
    request.setFileScarti(flagError);
    request.setFileIUV(flagImported);
    ImportStatusResponseDTO statusDTO = new ImportStatusResponseDTO();
    statusDTO.setDownloadUrls(downloadUrls);
    statusDTO.setStatus(status);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    when(ingestionFlowFileProcessingStatusServiceMock.getProcessingStatus(
      Mockito.same(userInfo), Mockito.same(accessToken), Mockito.eq(VALID_ORG_IPA_CODE), Mockito.eq(requestToken),
      Mockito.eq(IngestionFlowFile.IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
    )).thenReturn(statusDTO);

    // When
    PaaSILChiediStatoImportFlussoRisposta response =
      puForOrganizationPaymentsEndpoint.paaSILChiediStatoImportFlusso(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(IngestionFlowFileLegacyStatus.fromValue2LegacyValue(statusDTO.getStatus()), response.getStato());
    Assertions.assertEquals(expectedImportedUrl, response.getUrlFileIUV());
    Assertions.assertEquals(expectedErrorUrl, response.getUrlFileScarti());
    Assertions.assertEquals(expectedNoticeUrl, response.getUrlFileAvvisi());
  }

  private static Stream<Arguments> paaSILChiediStatoImportFlussoProvider() {
    String expectedUrl = "https://upload.url";
    DownloadUrl imported = new DownloadUrl(OUTPUT_FILE, expectedUrl + "/iuv");
    DownloadUrl errors = new DownloadUrl(DISCARDED_FILE, expectedUrl + "/errors");
    DownloadUrl notice = new DownloadUrl(PAYMENT_NOTICE_FILE, expectedUrl + "/notice");
    DownloadUrl input = new DownloadUrl(INPUT_FILE, expectedUrl + "/input");

    return Stream.of(
      Arguments.of(true, true, true, PROCESSING, null, null, null, null),
      Arguments.of(true, true, true, COMPLETED, List.of(imported, errors, notice), imported.getUrl(), errors.getUrl(), notice.getUrl()),
      Arguments.of(true, true, false, COMPLETED, List.of(imported, errors), imported.getUrl(), errors.getUrl(), null),
      Arguments.of(true, false, true, COMPLETED, List.of(imported, notice), imported.getUrl(), null, notice.getUrl()),
      Arguments.of(true, false, false, COMPLETED, List.of(imported), imported.getUrl(), null, null),
      Arguments.of(false, true, true, COMPLETED, List.of(errors, notice), null, errors.getUrl(), notice.getUrl()),
      Arguments.of(false, true, false, COMPLETED, List.of(errors), null, errors.getUrl(), null),
      Arguments.of(false, false, true, COMPLETED, List.of(notice), null, null, notice.getUrl()),
      Arguments.of(false, false, false, COMPLETED, List.of(), null, null, null),
      Arguments.of(false, false, false, COMPLETED, List.of(input), null, null, null)
    );
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

    ImportFileResponseDTO importFileResponseDTO = ImportFileResponseDTO.builder()
      .importId(String.valueOf(expectedToken))
      .uploadUrl(expectedUrl)
      .authorizationToken(HARDCODED_AUTHORIZATION_TOKEN)
      .importPath(HARDCODED_IMPORT_PATH)
      .build();
    when(ingestionFlowFileAuthorizationServiceMock.authorizeIngestionFlowFile(
      Mockito.same(userInfo), Mockito.same(accessToken), Mockito.eq(VALID_ORG_IPA_CODE), Mockito.eq(IngestionFlowFile.IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
    )).thenReturn(importFileResponseDTO);

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.PTDP_paaSILAutorizzaImportFlusso)
      .orgFiscalCode(VALID_ORGANIZATION_FISCAL_CODE)
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request, false);

    // When
    PaaSILAutorizzaImportFlussoRisposta response = puForOrganizationPaymentsEndpoint.paaSILAutorizzaImportFlusso(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(String.valueOf(expectedToken), response.getRequestToken());
    Assertions.assertEquals(expectedUrl, response.getUploadUrl());
    Assertions.assertEquals(HARDCODED_AUTHORIZATION_TOKEN, response.getAuthorizationToken());
    Assertions.assertEquals(HARDCODED_IMPORT_PATH, response.getImportPath());
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

    when(paaSILImportaDovutoServiceMock.handleAction(Mockito.same(request), Mockito.eq(VALID_ORG_IPA_CODE), Mockito.same(userInfo), Mockito.any()))
      .thenReturn(tripleResponse);

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.PTDP_paaSILImportaDovuto)
      .orgFiscalCode(VALID_ORGANIZATION_FISCAL_CODE)
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request, true);

    // When
    PaaSILImportaDovutoRisposta result = puForOrganizationPaymentsEndpoint.paaSILImportaDovuto(request, header);

    // Then
    Assertions.assertNotNull(result);
    verify(registryExtraInfoHandlerPaaSILImportaDovutoServiceMock).extractRequestExtraInfo(request, header);
    verify(registryExtraInfoHandlerPaaSILImportaDovutoServiceMock).extractResponseExtraInfo(result);
  }
  // endregion

  // region PaaSILInviaDovuti
  @Test
  void givenValidRequestWhenPaaSILInviaDovutiThenOk() throws Exception {
    // Given
    PaaSILInviaDovuti request = podamFactory.manufacturePojo(PaaSILInviaDovuti.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    Triple<PaaSILInviaDovutiRisposta, String, RegistryOutcome> expectedResponse = Triple.of(
      new PaaSILInviaDovutiRisposta(),
      "iuv",
      RegistryOutcome.OK
    );

    when(paaSILInviaDovutiServiceMock.processRequest(request, VALID_ORG_IPA_CODE, userInfo, "TOKEN"))
      .thenReturn(expectedResponse);

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.PTDP_paaSILInviaDovuti)
      .orgFiscalCode(VALID_ORGANIZATION_FISCAL_CODE)
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request, true);

    // When
    PaaSILInviaDovutiRisposta result = puForOrganizationPaymentsEndpoint.paaSILInviaDovuti(request, header);

    // Then
    Assertions.assertNotNull(result);
    verify(registryExtraInfoHandlerPaaSILInviaDovutiMock).extractRequestExtraInfo(request, header);
    verify(registryExtraInfoHandlerPaaSILInviaDovutiMock).extractResponseExtraInfo(result);
  }
  // endregion

  // region PaaSILInviaCarrelloDovuti
  @Test
  void givenValidRequestWhenPaaSILInviaCarrelloDovutiThenOk() throws Exception {
    // Given
    PaaSILInviaCarrelloDovuti request = podamFactory.manufacturePojo(PaaSILInviaCarrelloDovuti.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    Triple<PaaSILInviaCarrelloDovutiRisposta, String, RegistryOutcome> expectedResponse = Triple.of(
      new PaaSILInviaCarrelloDovutiRisposta(),
      "iuv",
      RegistryOutcome.OK
    );

    when(paaSILInviaCarrelloDovutiServiceMock.processRequest(request, VALID_ORG_IPA_CODE, userInfo, "TOKEN"))
      .thenReturn(expectedResponse);

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.PTDP_paaSILInviaCarrelloDovuti)
      .orgFiscalCode(VALID_ORGANIZATION_FISCAL_CODE)
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request, true);

    // When
    PaaSILInviaCarrelloDovutiRisposta result = puForOrganizationPaymentsEndpoint.paaSILInviaCarrelloDovuti(request, header);

    // Then
    Assertions.assertNotNull(result);
    verify(registryExtraInfoHandlerPaaSILInviaCarrelloDovutiMock).extractRequestExtraInfo(request, header);
    verify(registryExtraInfoHandlerPaaSILInviaCarrelloDovutiMock).extractResponseExtraInfo(result);
  }
  // endregion

  // region PaaSILVerificaAvviso
  @Test
  void givenValidRequestWhenPaaSPaaSILVerificaAvvisoThenOk() throws Exception {
    // Given
    PaaSILVerificaAvviso request = podamFactory.manufacturePojo(PaaSILVerificaAvviso.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    PaaSILVerificaAvvisoRisposta expectedResponse = new PaaSILVerificaAvvisoRisposta();

    when(paaSILVerificaAvvisoServiceMock.processRequest(request, VALID_ORG_IPA_CODE, userInfo, accessToken))
      .thenReturn(expectedResponse);

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.PTDP_paaSILVerificaAvviso)
      .orgFiscalCode(VALID_ORGANIZATION_FISCAL_CODE)
      .iuv(request.getIdentificativoUnivocoVersamento())
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request, false);

    // When
    PaaSILVerificaAvvisoRisposta result = puForOrganizationPaymentsEndpoint.paaSILVerificaAvviso(request, header);

    // Then
    Assertions.assertNotNull(result);
    Assertions. assertEquals(expectedResponse, result);
  }

  @Test
  void givenAnErrorWhenPaaSPaaSILVerificaAvvisoThenKo() throws Exception {
    // Given
    PaaSILVerificaAvviso request = podamFactory.manufacturePojo(PaaSILVerificaAvviso.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    when(paaSILVerificaAvvisoServiceMock.processRequest(request, VALID_ORG_IPA_CODE, userInfo, accessToken))
      .thenThrow(new SilFaultException(SilFaults.PAA_IUV_NON_VALIDO, "Description"));

    RegistryContextData expectedRegistryContextData = RegistryContextData.builder()
      .loggedUser(userInfo)
      .eventType(RegistryEventType.PTDP_paaSILVerificaAvviso)
      .orgFiscalCode(VALID_ORGANIZATION_FISCAL_CODE)
      .iuv(request.getIdentificativoUnivocoVersamento())
      .build();
    configureRegistryLoggerMock(expectedRegistryContextData, request, false);

    // When
    PaaSILVerificaAvvisoRisposta result = puForOrganizationPaymentsEndpoint.paaSILVerificaAvviso(request, header);

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.getFault());
    Assertions.assertEquals(SilFaults.PAA_IUV_NON_VALIDO.code(), result.getFault().getFaultCode());
    Assertions.assertEquals("Description", result.getFault().getDescription());
  }
  // endregion

  // region PaaSILChiediPagati
  @Test
  void givenValidRequestWhenPaaSILChiediPagatiThenOk() {
    // Given
    PaaSILChiediPagati request = podamFactory.manufacturePojo(PaaSILChiediPagati.class);
    PaaSILChiediPagatiRisposta expectedResponse = new PaaSILChiediPagatiRisposta();

    when(paaSILChiediPagatiServiceMock.processRequest(request, userInfo, accessToken))
      .thenReturn(expectedResponse);

    // When
    PaaSILChiediPagatiRisposta result = puForOrganizationPaymentsEndpoint.paaSILChiediPagati(request);

    // Then
    Assertions.assertNotNull(result);
    Assertions. assertEquals(expectedResponse, result);
  }

  @Test
  void givenAnErrorWhenPaaSPaaSILChiediPagatiThenKo() {
    // Given
    PaaSILChiediPagati request = podamFactory.manufacturePojo(PaaSILChiediPagati.class);

    when(paaSILChiediPagatiServiceMock.processRequest(request, userInfo, accessToken))
      .thenThrow(new SilFaultException(SilFaults.PAA_ID_SESSION_NON_VALIDO, "Description"));

    // When
    PaaSILChiediPagatiRisposta result = puForOrganizationPaymentsEndpoint.paaSILChiediPagati(request);

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.getFault());
    Assertions.assertEquals(SilFaults.PAA_ID_SESSION_NON_VALIDO.code(), result.getFault().getFaultCode());
    Assertions.assertEquals("Description", result.getFault().getDescription());
  }
  // endregion

  // region PaaSILChiediPagatiConRicevuta
  @Test
  void givenValidRequestWhenPaaSILChiediPagatiConRicevutaThenOk() {
    // Given
    PaaSILChiediPagatiConRicevuta request = podamFactory.manufacturePojo(PaaSILChiediPagatiConRicevuta.class);
    PaaSILChiediPagatiConRicevutaRisposta expectedResponse = new PaaSILChiediPagatiConRicevutaRisposta();

    when(paaSILChiediPagatiConRicevutaServiceMock.processRequest(request, userInfo, accessToken))
      .thenReturn(expectedResponse);

    // When
    PaaSILChiediPagatiConRicevutaRisposta result = puForOrganizationPaymentsEndpoint.paaSILChiediPagatiConRicevuta(request);

    // Then
    Assertions.assertNotNull(result);
    Assertions. assertEquals(expectedResponse, result);
  }

  @Test
  void givenAnErrorWhenPaaSILChiediPagatiConRicevutaThenKo() {
    // Given
    PaaSILChiediPagatiConRicevuta request = podamFactory.manufacturePojo(PaaSILChiediPagatiConRicevuta.class);

    when(paaSILChiediPagatiConRicevutaServiceMock.processRequest(request, userInfo, accessToken))
      .thenThrow(new SilFaultException(SilFaults.PAA_ID_SESSION_NON_VALIDO, "Description"));

    // When
    PaaSILChiediPagatiConRicevutaRisposta result = puForOrganizationPaymentsEndpoint.paaSILChiediPagatiConRicevuta(request);

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.getFault());
    Assertions.assertEquals(SilFaults.PAA_ID_SESSION_NON_VALIDO.code(), result.getFault().getFaultCode());
    Assertions.assertEquals("Description", result.getFault().getDescription());
  }
  // endregion

  // region PaaSILChiediEsitoCarrelloDovuti
  @Test
  void givenValidRequestWhenPaaSILChiediEsitoCarrelloDovutiThenOk() {
    // Given
    PaaSILChiediEsitoCarrelloDovuti request = podamFactory.manufacturePojo(PaaSILChiediEsitoCarrelloDovuti.class);
    PaaSILChiediEsitoCarrelloDovutiRisposta expectedResponse = new PaaSILChiediEsitoCarrelloDovutiRisposta();

    when(paaSILChiediEsitoCarrelloDovutiServiceMock.processRequest(request, userInfo, accessToken))
      .thenReturn(expectedResponse);

    // When
    PaaSILChiediEsitoCarrelloDovutiRisposta result = puForOrganizationPaymentsEndpoint.paaSILChiediEsitoCarrelloDovuti(request);

    // Then
    Assertions.assertNotNull(result);
    Assertions. assertEquals(expectedResponse, result);
  }

  @Test
  void givenAnErrorWhenPaaSILChiediEsitoCarrelloDovutiThenKo() {
    // Given
    PaaSILChiediEsitoCarrelloDovuti request = podamFactory.manufacturePojo(PaaSILChiediEsitoCarrelloDovuti.class);

    when(paaSILChiediEsitoCarrelloDovutiServiceMock.processRequest(request, userInfo, accessToken))
      .thenThrow(new SilFaultException(SilFaults.PAA_ID_SESSION_NON_VALIDO, "Description"));

    // When
    PaaSILChiediEsitoCarrelloDovutiRisposta result = puForOrganizationPaymentsEndpoint.paaSILChiediEsitoCarrelloDovuti(request);

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.getFault());
    Assertions.assertEquals(SilFaults.PAA_ID_SESSION_NON_VALIDO.code(), result.getFault().getFaultCode());
    Assertions.assertEquals("Description", result.getFault().getDescription());
  }
  // endregion

  // region PaaSILChiediStoricoPagamenti
  @Test
  void givenValidRequestWhenPaaSILChiediStoricoPagamentiThenOk() throws Exception {
    // Given
    PaaSILChiediStoricoPagamenti request = podamFactory.manufacturePojo(PaaSILChiediStoricoPagamenti.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    PaaSILChiediStoricoPagamentiRisposta expectedResponse = new PaaSILChiediStoricoPagamentiRisposta();

    when(paaSILChiediStoricoPagamentiServiceMock.processRequest(request, userInfo, accessToken))
      .thenReturn(expectedResponse);

    // When
    PaaSILChiediStoricoPagamentiRisposta result = puForOrganizationPaymentsEndpoint.paaSILChiediStoricoPagamenti(request, header);

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertEquals(expectedResponse, result);
  }

  @Test
  void givenAnErrorWhenPaaSILChiediStoricoPagamentiThenKo() throws Exception {
    // Given
    PaaSILChiediStoricoPagamenti request = podamFactory.manufacturePojo(PaaSILChiediStoricoPagamenti.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    when(paaSILChiediStoricoPagamentiServiceMock.processRequest(request, userInfo, accessToken))
      .thenThrow(new SilFaultException(SilFaults.PAA_ID_SESSION_NON_VALIDO, "Description"));

    // When
    PaaSILChiediStoricoPagamentiRisposta result = puForOrganizationPaymentsEndpoint.paaSILChiediStoricoPagamenti(request, header);

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.getFault());
    Assertions.assertEquals(SilFaults.PAA_ID_SESSION_NON_VALIDO.code(), result.getFault().getFaultCode());
    Assertions.assertEquals("Description", result.getFault().getDescription());
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
  void givenValidRequestWhenPaaSILChiediPosizioniAperteThenOk() {
    // Given
    PaaSILChiediPosizioniAperte request = podamFactory.manufacturePojo(PaaSILChiediPosizioniAperte.class);
    PaaSILChiediPosizioniAperteRisposta expectedResponse = new PaaSILChiediPosizioniAperteRisposta();

    when(paaSILChiediPosizioniAperteServiceMock.processRequest(request, userInfo, accessToken))
      .thenReturn(expectedResponse);

    // When
    PaaSILChiediPosizioniAperteRisposta result = puForOrganizationPaymentsEndpoint.paaSILChiediPosizioniAperte(request, null);

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertEquals(expectedResponse, result);
  }

  @Test
  void givenAnErrorWhenPaaSILChiediPosizioniAperteThenKo() {
    // Given
    PaaSILChiediPosizioniAperte request = podamFactory.manufacturePojo(PaaSILChiediPosizioniAperte.class);

    when(paaSILChiediPosizioniAperteServiceMock.processRequest(request, userInfo, accessToken))
      .thenThrow(new SilFaultException(SilFaults.PAA_ID_SESSION_NON_VALIDO, "Description"));

    // When
    PaaSILChiediPosizioniAperteRisposta result = puForOrganizationPaymentsEndpoint.paaSILChiediPosizioniAperte(request, null);

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.getFault());
    Assertions.assertEquals(SilFaults.PAA_ID_SESSION_NON_VALIDO.code(), result.getFault().getFaultCode());
    Assertions.assertEquals("Description", result.getFault().getDescription());
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
    request.setIdentificativoTipoDovuto("THAT_TYPE");
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    Long expectedToken = 12345L;

    when(paaSILPrenotaExportFlussoServiceMock.paaSILPrenotaExportFlusso(
      Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
    )).thenReturn(expectedToken);

    // When
    PaaSILPrenotaExportFlussoRisposta response = puForOrganizationPaymentsEndpoint.paaSILPrenotaExportFlusso(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(String.valueOf(expectedToken), response.getRequestToken());
  }

  @Test
  void givenRequestWithoutFromDateWhenPaaSILPrenotaExportFlussoThrowsClientExceptionThenResponseContainsExpectedFaultCode() throws Exception {
    PaaSILPrenotaExportFlusso request = podamFactory.manufacturePojo(PaaSILPrenotaExportFlusso.class);
    request.setDateFrom(null);
    request.setIdentificativoTipoDovuto("THAT_TYPE");
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    PaaSILPrenotaExportFlussoRisposta response = puForOrganizationPaymentsEndpoint.paaSILPrenotaExportFlusso(request, header);

    Assertions.assertNotNull(response);
    Assertions.assertEquals(SilFaults.PAA_DATE_FROM_NON_VALIDO.code(), response.getFault().getFaultCode());
  }

  @Test
  void givenRequestWithoutToDateWhenPaaSILPrenotaExportFlussoThrowsClientExceptionThenResponseContainsExpectedFaultCode() throws Exception {
    PaaSILPrenotaExportFlusso request = podamFactory.manufacturePojo(PaaSILPrenotaExportFlusso.class);
    request.setDateTo(null);
    request.setIdentificativoTipoDovuto("THAT_TYPE");
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    PaaSILPrenotaExportFlussoRisposta response = puForOrganizationPaymentsEndpoint.paaSILPrenotaExportFlusso(request, header);

    Assertions.assertNotNull(response);
    Assertions.assertEquals(SilFaults.PAA_DATE_TO_NON_VALIDO.code(), response.getFault().getFaultCode());
  }

  @Test
  void givenValidRequestWhenPaaSILPrenotaExportFlussoThrowsClientExceptionThenResponseContainsExpectedFaultCode() throws Exception {
    // Given
    PaaSILPrenotaExportFlusso request = podamFactory.manufacturePojo(PaaSILPrenotaExportFlusso.class);
    request.setIdentificativoTipoDovuto("THAT_TYPE");
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    when(paaSILPrenotaExportFlussoServiceMock.paaSILPrenotaExportFlusso(
      Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
    )).thenThrow(new ExportFileClientException(new InvalidValueException(
      ProcessExecutionsErrorDTO.CategoryEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE.getValue(), "Invalid time range"))
    );

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
    request.setIdentificativoTipoDovuto("THAT_TYPE");
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    when(paaSILPrenotaExportFlussoServiceMock.paaSILPrenotaExportFlusso(
      Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
    )).thenThrow(new ExportFileServiceException(ErrorCodeConstants.ERROR_CODE_INVALID_DEBT_POSITION_TYPE_ORG_CODE, "Identificativo tipo dovuto non valido"));

    // When
    PaaSILPrenotaExportFlussoRisposta response = puForOrganizationPaymentsEndpoint.paaSILPrenotaExportFlusso(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO.code(), response.getFault().getFaultCode());
  }
  // endregion

  // region PaaSILPrenotaExportFlussoIncrementaleConRicevuta
  @Test
  void givenValidRequestWhenPaaSILPrenotaExportFlussoIncrementaleConRicevutaThenResponseContainsExpectedToken() throws Exception {
    // Given
    PaaSILPrenotaExportFlussoIncrementaleConRicevuta request = podamFactory.manufacturePojo(PaaSILPrenotaExportFlussoIncrementaleConRicevuta.class);
    request.setIdentificativoTipoDovuto("THAT_TYPE");
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    Long expectedToken = 12345L;

    when(paaSILPrenotaExportFlussoIncrementaleConRicevutaServiceMock.doReservation(
      Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean()
    )).thenReturn(expectedToken);

    // When
    PaaSILPrenotaExportFlussoIncrementaleConRicevutaRisposta response = puForOrganizationPaymentsEndpoint
      .paaSILPrenotaExportFlussoIncrementaleConRicevuta(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(String.valueOf(expectedToken), response.getRequestToken());
  }

  @Test
  void givenValidRequestWhenPaaSILPrenotaExportFlussoIncrementaleConRicevutaThrowsClientExceptionThenResponseContainsExpectedFaultCode() throws Exception {
    // Given
    PaaSILPrenotaExportFlussoIncrementaleConRicevuta request = podamFactory.manufacturePojo(PaaSILPrenotaExportFlussoIncrementaleConRicevuta.class);
    request.setIdentificativoTipoDovuto("THAT_TYPE");
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    when(paaSILPrenotaExportFlussoIncrementaleConRicevutaServiceMock.doReservation(
      Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean()
    )).thenThrow(new ExportFileClientException(new InvalidValueException(
      ProcessExecutionsErrorDTO.CategoryEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE.getValue(), "Invalid time range"))
    );

    // When
    PaaSILPrenotaExportFlussoIncrementaleConRicevutaRisposta response = puForOrganizationPaymentsEndpoint
      .paaSILPrenotaExportFlussoIncrementaleConRicevuta(request, header);

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(SilFaults.PAA_INTERVALLO_DATE_NON_VALIDO.code(), response.getFault().getFaultCode());
  }

  @Test
  void givenValidRequestWhenPaaSILPrenotaExportFlussoIncrementaleConRicevutaThrowsServiceExceptionThenResponseContainsExpectedFaultCode() throws Exception {
    // Given
    PaaSILPrenotaExportFlussoIncrementaleConRicevuta request = podamFactory.manufacturePojo(PaaSILPrenotaExportFlussoIncrementaleConRicevuta.class);
    request.setIdentificativoTipoDovuto("THAT_TYPE");
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    when(paaSILPrenotaExportFlussoIncrementaleConRicevutaServiceMock.doReservation(
      Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean()
    )).thenThrow(new ExportFileServiceException(ErrorCodeConstants.ERROR_CODE_INVALID_DEBT_POSITION_TYPE_ORG_CODE, "Identificativo tipo dovuto non valido"));

    // When
    PaaSILPrenotaExportFlussoIncrementaleConRicevutaRisposta response = puForOrganizationPaymentsEndpoint
      .paaSILPrenotaExportFlussoIncrementaleConRicevuta(request, header);

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
