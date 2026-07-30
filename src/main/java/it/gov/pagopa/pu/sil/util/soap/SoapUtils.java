package it.gov.pagopa.pu.sil.util.soap;

import it.gov.pagopa.pu.sil.exception.InvalidValueException;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import jakarta.xml.bind.JAXBContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.soap.SoapHeaderElement;

import java.util.Optional;
import java.util.function.Function;

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
      throw new InvalidValueException(ErrorCodeConstants.ERROR_CODE_XML_UNMARSHALLING_ERROR, "Error unmarshalling header", e);
    }
  }

  public static <H> String getOrganizationIpaCodeFromHeader(
      SoapHeaderElement header,
      Class<H> headerClass,
      Function<H, String> orgIpaCodeExtractor,
      String operationName) {
    H intestazionePPT = unmarshallHeader(header, headerClass);
    String orgIpaCode = Optional.ofNullable(intestazionePPT)
        .map(orgIpaCodeExtractor)
        .orElse(null);
    log.info("processing {} orgIpaCode[{}]", operationName, orgIpaCode);
    return orgIpaCode;
  }
}
