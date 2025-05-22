package it.gov.pagopa.pu.sil.util.soap;

import it.gov.pagopa.pu.sil.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ws.soap.SoapHeaderElement;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.JAXBElement;
import javax.xml.transform.Source;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
            ApplicationException ex = assertThrows(ApplicationException.class, () ->
                SoapUtils.unmarshallHeader(header, String.class)
            );
            assertTrue(ex.getMessage().contains("error unmarshalling header"));
        }
    }
}
