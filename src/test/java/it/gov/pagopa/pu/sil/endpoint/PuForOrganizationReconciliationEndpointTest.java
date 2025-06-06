package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.sil.enums.RegistrySilEventType;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.IngestionFlowFileTypeValidationException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileAuthorizationService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.pivot.ente.*;
import it.veneto.regione.pagamenti.pivot.ente.ppthead.IntestazionePPT;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.ws.soap.SoapHeaderElement;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class PuForOrganizationReconciliationEndpointTest {

  private static final String VALID_ORG_IPA_CODE = "IPA_2";
  private static final String INVALID_ORG_IPA_CODE = "IPA_1";

  @Mock
  private RegistryLogger registryLoggerMock;
  @Mock
  private IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationServiceMock;
  @InjectMocks
  private PuForOrganizationReconciliationEndpoint puForOrganizationReconciliationEndpoint;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  private static void configureSecurityContext(UserInfo expectedUserInfo) {
    SecurityContextHolder.setContext(new SecurityContextImpl(new UsernamePasswordAuthenticationToken(expectedUserInfo, "token")));
  }

  @BeforeEach
  void clearContexts() {
    RequestContextHolder.resetRequestAttributes();
    SecurityContextHolder.clearContext();

    //set a valid user for org IPA_2
    UserInfo expectedUserInfo = new UserInfo();
    expectedUserInfo.setMappedExternalUserId("USERID");
    expectedUserInfo.setOrganizations(List.of(
      new UserOrganizationRoles("OID1", 1L, INVALID_ORG_IPA_CODE, "CF_1", "email", List.of("")),
      new UserOrganizationRoles("OID2", 2L, VALID_ORG_IPA_CODE, "CF_2", "email", List.of(SecurityUtils.OPERATOR_ROLE_ADMIN))
    ));
    configureSecurityContext(expectedUserInfo);
  }


  //region pivotSILAutorizzaImportFlusso

  @Test
  void givenValidRequestWhenPivotSILAutorizzaImportFlussoThenRegistryLoggerInvoked() throws Exception {
    PivotSILAutorizzaImportFlusso request = podamFactory.manufacturePojo(PivotSILAutorizzaImportFlusso.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    Mockito.when(registryLoggerMock.execute(Mockito.any(), Mockito.eq(RegistrySilEventType.pivotSILAutorizzaImportFlusso), Mockito.any(),
        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
      .thenReturn(new PivotSILAutorizzaImportFlussoRisposta());

    PivotSILAutorizzaImportFlussoRisposta response =
            puForOrganizationReconciliationEndpoint.pivotSILAutorizzaImportFlusso(request, header);

    Assertions.assertNotNull(response);
    Assertions.assertNull(response.getFault());
  }

  @Test
  void givenValidRequestWhenPivotSILAutorizzaImportFlussoThenResponseContainsExpectedTokenAndUrl() throws Exception {
    PivotSILAutorizzaImportFlusso request = podamFactory.manufacturePojo(PivotSILAutorizzaImportFlusso.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    Long expectedToken = 98765L;
    String expectedUrl = "https://upload.pivot.url";
    Mockito.when(ingestionFlowFileAuthorizationServiceMock.authorizeIngestionFlowFile(
      Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
    )).thenReturn(Pair.of(expectedToken, expectedUrl));
    Mockito.when(registryLoggerMock.execute(Mockito.any(), Mockito.eq(RegistrySilEventType.pivotSILAutorizzaImportFlusso), Mockito.any(),
        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
      .thenCallRealMethod();

    PivotSILAutorizzaImportFlussoRisposta response =
            puForOrganizationReconciliationEndpoint.pivotSILAutorizzaImportFlusso(request, header);

    Assertions.assertNotNull(response);
    Assertions.assertEquals(String.valueOf(expectedToken), response.getRequestToken());
    Assertions.assertEquals(expectedUrl, response.getUploadUrl());
  }

  //endregion

  //region pivotSILAutorizzaImportFlussoTesoreria

  @Test
  void givenValidRequestWhenPivotSILAutorizzaImportFlussoTesoreriaThenResponseContainsExpectedTokenAndUrl() throws Exception {
    PivotSILAutorizzaImportFlussoTesoreria request = podamFactory.manufacturePojo(PivotSILAutorizzaImportFlussoTesoreria.class);
    request.setTipoFlusso(IngestionFlowFileTypeEnum.TREASURY_OPI.name());
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    Long expectedToken = 98765L;
    String expectedUrl = "https://upload.pivot.url";
    Mockito.when(ingestionFlowFileAuthorizationServiceMock.authorizeTreasuryIngestionFlowFile(
      Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq(IngestionFlowFileTypeEnum.TREASURY_OPI)
    )).thenReturn(Pair.of(expectedToken, expectedUrl));
    Mockito.when(registryLoggerMock.execute(Mockito.any(), Mockito.eq(RegistrySilEventType.pivotSILAutorizzaImportFlussoTesoreria), Mockito.any(),
        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
      .thenCallRealMethod();

    PivotSILAutorizzaImportFlussoTesoreriaRisposta response =
      puForOrganizationReconciliationEndpoint.pivotSILAutorizzaImportFlussoTesoreria(request, header);

    Assertions.assertNotNull(response);
    Assertions.assertEquals(String.valueOf(expectedToken), response.getRequestToken());
    Assertions.assertEquals(expectedUrl, response.getUploadUrl());
  }

  @Test
  void givenIngestionFlowFileTypeValidationExceptionWhenPivotSILAutorizzaImportFlussoTesoreriaThenCustomHandlerIsUsed() throws Exception {
    PivotSILAutorizzaImportFlussoTesoreria request = podamFactory.manufacturePojo(PivotSILAutorizzaImportFlussoTesoreria.class);
    request.setTipoFlusso(IngestionFlowFileTypeEnum.DP_INSTALLMENTS.name());
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    String customMessage = "Tipo flusso non valido";
    Mockito.when(ingestionFlowFileAuthorizationServiceMock.authorizeTreasuryIngestionFlowFile(
      Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq(IngestionFlowFileTypeEnum.DP_INSTALLMENTS)
    )).thenThrow(new IngestionFlowFileTypeValidationException(customMessage));
    Mockito.when(registryLoggerMock.execute(Mockito.any(), Mockito.eq(RegistrySilEventType.pivotSILAutorizzaImportFlussoTesoreria), Mockito.any(),
        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
      .thenCallRealMethod();

    PivotSILAutorizzaImportFlussoTesoreriaRisposta response =
      puForOrganizationReconciliationEndpoint.pivotSILAutorizzaImportFlussoTesoreria(request, header);

    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PIVOT_TIPO_FLUSSO_NON_VALIDO.code(), response.getFault().getFaultCode());
    Assertions.assertTrue(response.getFault().getDescription().contains(customMessage));
  }

  @Test
  void givenGenericExceptionWhenPivotSILAutorizzaImportFlussoTesoreriaThenBaseHandlerIsUsed() throws Exception {
    PivotSILAutorizzaImportFlussoTesoreria request = podamFactory.manufacturePojo(PivotSILAutorizzaImportFlussoTesoreria.class);
    request.setTipoFlusso(IngestionFlowFileTypeEnum.TREASURY_OPI.name());
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(INVALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    Mockito.when(ingestionFlowFileAuthorizationServiceMock.authorizeTreasuryIngestionFlowFile(
      Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq(IngestionFlowFileTypeEnum.TREASURY_OPI)
    )).thenThrow(new UnauthorizedException("Utente non autorizzato"));
    Mockito.when(registryLoggerMock.execute(Mockito.any(), Mockito.eq(RegistrySilEventType.pivotSILAutorizzaImportFlussoTesoreria), Mockito.any(),
        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
      .thenCallRealMethod();

    PivotSILAutorizzaImportFlussoTesoreriaRisposta response =
      puForOrganizationReconciliationEndpoint.pivotSILAutorizzaImportFlussoTesoreria(request, header);

    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PIVOT_ENTE_NON_VALIDO.code(), response.getFault().getFaultCode());
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
