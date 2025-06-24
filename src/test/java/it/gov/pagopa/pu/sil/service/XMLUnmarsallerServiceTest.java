package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.sil.exception.InvalidValueException;
import it.veneto.regione.schemas._2012.pagamenti.ente.Bilancio;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtAccertamento;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtCapitolo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.math.BigDecimal;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class XMLUnmarsallerServiceTest {

  private XMLUnmarshallerService service;
  private JAXBContext jaxbContext;
  private Schema schema;


  @BeforeEach
  void setUp() throws JAXBException {
    service = new XMLUnmarshallerService();
    jaxbContext = JAXBContext.newInstance(Bilancio.class);

    try {
      URL xsdUrl = getClass().getResource("soap/wsdl/payments/PagInf_Dovuti_Pagati_6_2_0.xsd");
      if (xsdUrl != null) {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schema = schemaFactory.newSchema(xsdUrl);
      }
    } catch (Exception e) {
      throw new RuntimeException("Error initializing Schema for testing", e);
    }
  }

  @Test
  void unmarshal_returnsObjectOnValidXml() {
    String xmlString = "<bilancio>" +
      "<capitolo>" +
      "<codCapitolo>CAP1</codCapitolo>" +
      "<codUfficio>UFF1</codUfficio>" +
      "<accertamento>" +
      "<codAccertamento>ACC1</codAccertamento>" +
      "<importo>100.00</importo>" +
      "</accertamento>" +
      "</capitolo>" +
      "</bilancio>";

    Bilancio expectedBilancio = new Bilancio();
    CtCapitolo capitolo = new CtCapitolo();
    capitolo.setCodCapitolo("CAP1");
    capitolo.setCodUfficio("UFF1");
    CtAccertamento accertamento = new CtAccertamento();
    accertamento.setCodAccertamento("ACC1");
    accertamento.setImporto(new BigDecimal("100.00"));
    capitolo.getAccertamentos().add(accertamento);
    expectedBilancio.getCapitolos().add(capitolo);

    Bilancio result = service.unmarshal(xmlString, Bilancio.class, jaxbContext, schema,"http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/");

    assertNotNull(result);
    assertEquals(expectedBilancio.getCapitolos().size(), result.getCapitolos().size());
    assertEquals(expectedBilancio.getCapitolos().get(0).getCodCapitolo(), result.getCapitolos().get(0).getCodCapitolo());
    assertEquals(expectedBilancio.getCapitolos().get(0).getCodUfficio(), result.getCapitolos().get(0).getCodUfficio());
    assertEquals(expectedBilancio.getCapitolos().get(0).getAccertamentos().size(), result.getCapitolos().get(0).getAccertamentos().size());
    assertEquals(expectedBilancio.getCapitolos().get(0).getAccertamentos().get(0).getCodAccertamento(), result.getCapitolos().get(0).getAccertamentos().get(0).getCodAccertamento());
    assertEquals(expectedBilancio.getCapitolos().get(0).getAccertamentos().get(0).getImporto(), result.getCapitolos().get(0).getAccertamentos().get(0).getImporto());
  }

  @Test
  void unmarshal_throwsInvalidValueExceptionOnInvalidXml() {
    String xmlString = "invalid.xml";

    assertThrows(InvalidValueException.class, () -> {
      service.unmarshal(xmlString, Bilancio.class, jaxbContext, schema, "http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/");
    });
  }

  @Test
  void unmarshal_throwsInvalidValueExceptionOnIOException() {
    String xmlString = "nonexistent.xml";

    assertThrows(InvalidValueException.class, () -> {
      service.unmarshal(xmlString, Bilancio.class, jaxbContext, schema, "http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/");
    });
  }
}
