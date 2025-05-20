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

  private final PodamFactory podamFactory;

  PuForOrganizationPaymentsEndpointTest() {
    podamFactory = TestUtils.getPodamFactory();
  }

  //region paaSILAutorizzaImportFlusso

  @Test
  void givenAnyWhenPaaSILAutorizzaImportFlussoThenFault() throws Exception {
    // given
    PaaSILAutorizzaImportFlusso request = podamFactory.manufacturePojo(PaaSILAutorizzaImportFlusso.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    SoapHeaderElement header =  TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    // when
    PaaSILAutorizzaImportFlussoRisposta response = puForOrganizationPaymentsEndpoint.paaSILAutorizzaImportFlusso(request, header);

    // verify
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PAA_SYSTEM_ERROR.code(), response.getFault().getFaultCode());
  }

  //endregion

  //region pivotSILAutorizzaImportFlusso

  @Test
  void givenAnyWhenPivotSILAutorizzaImportFlussoThenFault() throws Exception {
    // given
    PivotSILAutorizzaImportFlusso request = podamFactory.manufacturePojo(PivotSILAutorizzaImportFlusso.class);
    it.veneto.regione.pagamenti.pivot.ente.ppthead.IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(it.veneto.regione.pagamenti.pivot.ente.ppthead.IntestazionePPT.class);
    SoapHeaderElement header =  TestUtils.createSoapHeaderElement(intestazionePPT, it.veneto.regione.pagamenti.pivot.ente.ppthead.IntestazionePPT.class);

    // when
    PivotSILAutorizzaImportFlussoRisposta response = puForOrganizationPivotEndpoint.pivotSILAutorizzaImportFlusso(request, header);

    // verify
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PIVOT_SYSTEM_ERROR.code(), response.getFault().getFaultCode());
  }

  //endregion

  //region paaSILChiediAvvisiPendenti
  @Test
  void givenAnyWhenPaaSILChiediAvvisiPendentiThenFaultThenFault() throws Exception {
    // given
    PaaSILChiediAvvisiPendenti request = podamFactory.manufacturePojo(PaaSILChiediAvvisiPendenti.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    SoapHeaderElement header =  TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    // when
    PaaSILChiediAvvisiPendentiRisposta response = puForOrganizationPayEndpoint.paaSILChiediAvvisiPendenti(request, header);

    // verify
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PAA_SYSTEM_ERROR.code(), response.getFault().getFaultCode());
  }

  //endregion

  //region paaSILChiediPosizioniAperte
  @Test
  void givenAnyWhenPaaSILChiediPosizioniAperteThenFault() throws Exception {
    // given
    PaaSILChiediPosizioniAperte request = podamFactory.manufacturePojo(PaaSILChiediPosizioniAperte.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    SoapHeaderElement header =  TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    // when
    PaaSILChiediPosizioniAperteRisposta response = puForOrganizationPayEndpoint.paaSILChiediPosizioniAperte(request, header);
    // verify
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PAA_SYSTEM_ERROR.code(), response.getFault().getFaultCode());
  }

  //endregion

  //region paaSILChiediStoricoPagamenti
  @Test
  void givenAnyWhenPaaSILChiediStoricoPagamentiThenFault() throws Exception {
    // given
    PaaSILChiediStoricoPagamenti request = podamFactory.manufacturePojo(PaaSILChiediStoricoPagamenti.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    SoapHeaderElement header =  TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    // when
    PaaSILChiediStoricoPagamentiRisposta response = puForOrganizationPayEndpoint.paaSILChiediStoricoPagamenti(request, header);
    // verify
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PAA_SYSTEM_ERROR.code(), response.getFault().getFaultCode());
  }

  //endregion

  //region paaSILRegistraPagamento
  @Test
  void givenAnyWhenPaaSILRegistraPagamentoThenFault() throws Exception {
    // given
    PaaSILRegistraPagamento request = podamFactory.manufacturePojo(PaaSILRegistraPagamento.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    SoapHeaderElement header =  TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);
    // when
    PaaSILRegistraPagamentoRisposta response = puForOrganizationPayEndpoint.paaSILRegistraPagamento(request, header);
    // verify
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PAA_SYSTEM_ERROR.code(), response.getFault().getFaultCode());
  }

  //endregion
}
