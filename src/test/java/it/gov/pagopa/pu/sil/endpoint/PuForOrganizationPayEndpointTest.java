package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILAutorizzaImportFlusso;
import it.veneto.regione.pagamenti.ente.PaaSILAutorizzaImportFlussoRisposta;
import it.veneto.regione.pagamenti.ente.ppthead.IntestazionePPT;
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
}
