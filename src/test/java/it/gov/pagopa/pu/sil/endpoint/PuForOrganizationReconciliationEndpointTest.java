package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILAutorizzaImportFlusso;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILAutorizzaImportFlussoRisposta;
import it.veneto.regione.pagamenti.pivot.ente.ppthead.IntestazionePPT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.soap.SoapHeaderElement;
import uk.co.jemos.podam.api.PodamFactory;

@ExtendWith(MockitoExtension.class)
class PuForOrganizationReconciliationEndpointTest {
  @InjectMocks
  private PuForOrganizationReconciliationEndpoint puForOrganizationReconciliationEndpoint;

  private final PodamFactory podamFactory;

  PuForOrganizationReconciliationEndpointTest() {
    podamFactory = TestUtils.getPodamFactory();
  }

  //region pivotSILAutorizzaImportFlusso

  @Test
  void givenAnyWhenPivotSILAutorizzaImportFlussoThenFault() throws Exception {
    // given
    PivotSILAutorizzaImportFlusso request = podamFactory.manufacturePojo(PivotSILAutorizzaImportFlusso.class);
    IntestazionePPT intestazionePPT = podamFactory.manufacturePojo(IntestazionePPT.class);
    SoapHeaderElement header =  TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    // when
    PivotSILAutorizzaImportFlussoRisposta response = puForOrganizationReconciliationEndpoint.pivotSILAutorizzaImportFlusso(request, header);

    // verify
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getFault());
    Assertions.assertEquals(SilFaults.PIVOT_SYSTEM_ERROR.code(), response.getFault().getFaultCode());
  }

  //endregion
}
