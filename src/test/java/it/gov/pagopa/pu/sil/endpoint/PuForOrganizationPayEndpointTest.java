package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILAutorizzaImportFlusso;
import it.veneto.regione.pagamenti.ente.PaaSILAutorizzaImportFlussoRisposta;
import it.veneto.regione.pagamenti.ente.ppthead.IntestazionePPT;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILAutorizzaImportFlusso;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILAutorizzaImportFlussoRisposta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.soap.SoapHeaderElement;
import uk.co.jemos.podam.api.PodamFactory;

@ExtendWith(MockitoExtension.class)
class PuForOrganizationPayEndpointTest {

  @InjectMocks
  private PuForOrganizationPayEndpoint puForOrganizationPayEndpoint;
  @InjectMocks
  private PuForOrganizationPivotEndpoint puForOrganizationPivotEndpoint;

  private final PodamFactory podamFactory;

  PuForOrganizationPayEndpointTest() {
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
    PaaSILAutorizzaImportFlussoRisposta response = puForOrganizationPayEndpoint.paaSILAutorizzaImportFlusso(request, header);

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
}
