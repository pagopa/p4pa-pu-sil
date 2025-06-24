package it.gov.pagopa.pu.sil.service;

import it.veneto.regione.schemas._2012.pagamenti.ente.Bilancio;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceUnmashallerServiceTest {


  private Resource resource;
  private BalanceUnmashallerService handler;
  private XMLUnmarshallerService xmlUnmarshallerService;
  private static final String XML_STRING_CAPITOLO =  "<capitolo>" +
    "<codCapitolo>CAP1</codCapitolo>" +
    "<codUfficio>UFF1</codUfficio>" +
    "<accertamento>" +
    "<codAccertamento>ACC1</codAccertamento>" +
    "<importo>100.00</importo>" +
    "</accertamento>" +
    "</capitolo>";

  private static final String XML_STRING_BILANCIO_WITHOUT_NAMESPACE = "<bilancio>" +
    XML_STRING_CAPITOLO +
    "</bilancio>";


  private static final String XML_STRING_BILANCIO_WITH_NAMESPACE = "<bilancio  xmlns=\"http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/\">" +
    XML_STRING_CAPITOLO +
    "</bilancio>";

  @BeforeEach
  void setUp() {
    xmlUnmarshallerService = new XMLUnmarshallerService();
    resource = new ClassPathResource("soap/wsdl/payments/PagInf_Dovuti_Pagati_6_2_0.xsd");
    handler = new BalanceUnmashallerService(resource, xmlUnmarshallerService);
  }


  @Test
  void testHandleValidXmlWithNamespace() {

    //when
    Bilancio result = handler.unmarshal(XML_STRING_BILANCIO_WITH_NAMESPACE);

    // then
    assertNotNull(result);
    assertEquals("CAP1", result.getCapitolos().getFirst().getCodCapitolo());
    assertEquals("UFF1", result.getCapitolos().getFirst().getCodUfficio());
  }

  @Test
  void testHandleValidXmlWithoutNamespace() {

    //when
    Bilancio result = handler.unmarshal(XML_STRING_BILANCIO_WITHOUT_NAMESPACE);

    // then
    assertNotNull(result);
    assertEquals("CAP1", result.getCapitolos().getFirst().getCodCapitolo());
    assertEquals("UFF1", result.getCapitolos().getFirst().getCodUfficio());
  }

  @Test
  void testJAXBExceptionInConstructor() {
    try(MockedStatic<JAXBContext> mockedStaticJAXBContext = Mockito.mockStatic(JAXBContext.class)) {
      mockedStaticJAXBContext.when(() -> JAXBContext.newInstance(Bilancio.class))
        .thenThrow(new JAXBException("Simulated JAXBException"));
      assertThrows(IllegalStateException.class, () -> new BalanceUnmashallerService(resource, null));
    }
  }

  @Test
  void testIOExceptionInConstructor() throws IOException {
    // given
    Resource mockResource = mock(Resource.class);
    when(mockResource.getURL()).thenThrow(new IOException("Simulated IOException"));

    // when then
    assertThrows(IllegalStateException.class, () -> new BalanceUnmashallerService(mockResource, null));
  }



}
