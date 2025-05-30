package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.sil.enums.RegistrySilEventType;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.registry.extrainfo.RegistryExtraInfoHandlerPaaSILImportaDovuto;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.paasillimportadovuto.PaaSILImportaDovutoService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.*;
import it.veneto.regione.pagamenti.ente.ppthead.IntestazionePPT;
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
class PuForOrganizationPaymentsEndpointTest {

  private static final String VALID_ORG_IPA_CODE = "IPA_2";
  private static final String INVALID_ORG_IPA_CODE = "IPA_1";

  @Mock
  private RegistryLogger registryLoggerMock;

  @Mock
  private PaaSILImportaDovutoService paaSILImportaDovutoServiceMock;
  @Mock
  private RegistryExtraInfoHandlerPaaSILImportaDovuto registryExtraInfoHandlerPaaSILImportaDovutoServiceMock;

  @InjectMocks
  private PuForOrganizationPaymentsEndpoint puForOrganizationPaymentsEndpoint;

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

  // region PaaSILAutorizzaImportFlusso
  @Test
  void givenInvalidUserWhenPaaSILAutorizzaImportFlussoThenFault() throws Exception {
    // Given
    PaaSILAutorizzaImportFlusso request = podamFactory.manufacturePojo(PaaSILAutorizzaImportFlusso.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(INVALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    // when
    PaaSILAutorizzaImportFlussoRisposta response = puForOrganizationPaymentsEndpoint.paaSILAutorizzaImportFlusso(request, header);

    // verify
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PAA_ENTE_NON_VALIDO.code(), response.getFault().getFaultCode());
    Assertions.assertEquals(SilFaults.PAA_ENTE_NON_VALIDO.description(), response.getFault().getFaultString());
    Assertions.assertEquals("Utente non autorizzato", response.getFault().getDescription());
    Assertions.assertNull(response.getAuthorizationToken());
    Assertions.assertNull(response.getImportPath());
    Assertions.assertNull(response.getRequestToken());
    Assertions.assertNull(response.getUploadUrl());
  }

  @Test
  void givenAnyWhenPaaSILAutorizzaImportFlussoThenFault() throws Exception {
    testFaultResponse(PaaSILAutorizzaImportFlusso.class,
      SilFaults.PAA_SYSTEM_ERROR.code(),
      puForOrganizationPaymentsEndpoint::paaSILAutorizzaImportFlusso);
  }

  // endregion

  // region PasSILImportaDovuto

  @Test
  void givenValidRequestWhenPaaSILImportaDovutoThenRegistryLoggerInvoked() throws Exception {
    // Given
    PaaSILImportaDovuto request = podamFactory.manufacturePojo(PaaSILImportaDovuto.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    intestazionePPT.setCodIpaEnte(VALID_ORG_IPA_CODE);
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    Mockito.when(registryLoggerMock.execute(Mockito.any(), Mockito.eq(RegistrySilEventType.paaSILImportaDovuto), Mockito.any(),
        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
      .thenReturn(new PaaSILImportaDovutoRisposta());

    // When
    PaaSILImportaDovutoRisposta response = puForOrganizationPaymentsEndpoint.paaSILImportaDovuto(request, header);

    // Then
    Assertions.assertNotNull(response);
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
  void givenAnyWhenPaaSILChiediStoricoPagamentiThenFault() throws Exception {
    testFaultResponse(PaaSILChiediStoricoPagamenti.class,
      SilFaults.PAA_SYSTEM_ERROR.code(),
      puForOrganizationPaymentsEndpoint::paaSILChiediStoricoPagamenti);
  }
  // endregion

  // region PaaSILRegistraPagamento
  @Test
  void givenAnyWhenPaaSILRegistraPagamentoThenFault() throws Exception {
    testFaultResponse(PaaSILRegistraPagamento.class,
      SilFaults.PAA_SYSTEM_ERROR.code(),
      puForOrganizationPaymentsEndpoint::paaSILRegistraPagamento);
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
