package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.*;
import it.veneto.regione.pagamenti.ente.ppthead.IntestazionePPT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.soap.SoapHeaderElement;
import uk.co.jemos.podam.api.PodamFactory;

@ExtendWith(MockitoExtension.class)
class PuForOrganizationPaymentsEndpointTest {

  @InjectMocks
  private PuForOrganizationPaymentsEndpoint puForOrganizationPaymentsEndpoint;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  // region PaaSILAutorizzaImportFlusso
  @Test
  void givenAnyWhenPaaSILAutorizzaImportFlussoThenFault() throws Exception {
    testFaultResponse(PaaSILAutorizzaImportFlusso.class,
                      SilFaults.PAA_SYSTEM_ERROR.code(),
                      puForOrganizationPaymentsEndpoint::paaSILAutorizzaImportFlusso);
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
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    R response = testFunction.apply(request, header);

    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(faultCode, response.getFault().getFaultCode());
  }
}
