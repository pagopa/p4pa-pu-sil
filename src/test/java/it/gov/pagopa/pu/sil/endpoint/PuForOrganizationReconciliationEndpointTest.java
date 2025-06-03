package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.sil.enums.RegistrySilEventType;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.pivot.ente.*;
import it.veneto.regione.pagamenti.pivot.ente.ppthead.IntestazionePPT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.soap.SoapHeaderElement;
import uk.co.jemos.podam.api.PodamFactory;

@ExtendWith(MockitoExtension.class)
class PuForOrganizationReconciliationEndpointTest {
  @Mock
  private RegistryLogger registryLoggerMock;

  @InjectMocks
  private PuForOrganizationReconciliationEndpoint puForOrganizationReconciliationEndpoint;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

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

  //endregion

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
