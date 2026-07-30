package it.gov.pagopa.pu.sil.util.soap;

import it.gov.pagopa.pu.sil.exception.InvalidValueException;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.pivot.ente.ppthead.IntestazionePPT;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ws.soap.SoapHeaderElement;

import javax.xml.transform.Source;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SoapUtilsTest {

  @Test
  void unmarshallHeader_ReturnsNullWhenHeaderIsNull() {
    assertNull(SoapUtils.unmarshallHeader(null, String.class));
  }

  @Test
  void unmarshallHeader_ReturnsUnmarshalledValueOnValidHeader() throws Exception {
    SoapHeaderElement header = mock(SoapHeaderElement.class);
    Source source = mock(Source.class);
    when(header.getSource()).thenReturn(source);

    JAXBContext jaxbContext = mock(JAXBContext.class);
    Unmarshaller unmarshaller = mock(Unmarshaller.class);
    JAXBElement<String> jaxbElement = mock(JAXBElement.class);

    when(jaxbContext.createUnmarshaller()).thenReturn(unmarshaller);
    when(unmarshaller.unmarshal(source, String.class)).thenReturn(jaxbElement);
    when(jaxbElement.getValue()).thenReturn("value");

    try (var contextMockedStatic = Mockito.mockStatic(JAXBContext.class)) {
      contextMockedStatic.when(() -> JAXBContext.newInstance(String.class)).thenReturn(jaxbContext);
      assertEquals("value", SoapUtils.unmarshallHeader(header, String.class));
    }
  }

  @Test
  void unmarshallHeader_ThrowsApplicationExceptionOnError() throws Exception {
    SoapHeaderElement header = mock(SoapHeaderElement.class);
    Source source = mock(Source.class);
    when(header.getSource()).thenReturn(source);

    JAXBContext jaxbContext = mock(JAXBContext.class);
    when(jaxbContext.createUnmarshaller()).thenThrow(new RuntimeException("fail"));

    try (var contextMockedStatic = Mockito.mockStatic(JAXBContext.class)) {
      contextMockedStatic.when(() -> JAXBContext.newInstance(String.class)).thenReturn(jaxbContext);
      InvalidValueException ex = assertThrows(InvalidValueException.class, () ->
        SoapUtils.unmarshallHeader(header, String.class)
      );
      assertEquals(ErrorCodeConstants.ERROR_CODE_XML_UNMARSHALLING_ERROR, ex.getCode());
      assertTrue(ex.getMessage().contains("Error unmarshalling header"));
    }
  }

  @Test
  void testGetOrganizationIpaCodeFromHeader() throws Exception {
    IntestazionePPT intestazionePPT = new IntestazionePPT();
    intestazionePPT.setCodIpaEnte("IPA12345");
    SoapHeaderElement header = TestUtils.createSoapHeaderElement(intestazionePPT, IntestazionePPT.class);

    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header, IntestazionePPT.class, IntestazionePPT::getCodIpaEnte, "operationName");

    assertEquals(intestazionePPT.getCodIpaEnte(), orgIpaCode);
  }
}
