package it.gov.pagopa.pu.sil.util.soap;

import it.gov.pagopa.pu.sil.exception.ApplicationException;
import jakarta.xml.bind.JAXBContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.soap.SoapHeaderElement;

@Slf4j
public class SoapUtils {

  private SoapUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static <T> T unmarshallHeader(SoapHeaderElement header, Class<T> type) {
    try {
      if (header == null) return null;
      return JAXBContext.newInstance(type).createUnmarshaller().unmarshal(header.getSource(), type).getValue();
    } catch (Exception e) {
      log.error("error unmarshalling header", e);
      throw new ApplicationException("error unmarshalling header", e);
    }
  }


}
