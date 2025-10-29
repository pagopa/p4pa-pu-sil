package it.gov.pagopa.pu.sil.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionErrorDTO;
import it.gov.pagopa.pu.sil.config.rest.RestTemplateConfig;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApiClientConfig;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

@Slf4j
public class DebtPositionsResponseErrorHandler extends
  DefaultResponseErrorHandler {

  private static final String DEBT_POSITIONS = "DEBT-POSITIONS";
  private static final Pattern ERROR_CODE_PATTERN = Pattern.compile(
    "^\\[([A-Z0-9_]+)]\\s+.*");

  private final ResponseErrorHandler errorLoggerHandler;
  private final ObjectMapper objectMapper;

  public DebtPositionsResponseErrorHandler(
    DebtPositionsApiClientConfig clientConfig,
    ObjectMapper objectMapper) {
    this.errorLoggerHandler = clientConfig.isPrintBodyWhenError() ?
      RestTemplateConfig.bodyPrinterWhenError(DEBT_POSITIONS)
      : null;
    this.objectMapper = objectMapper;
  }

  @Override
  public void handleError(ClientHttpResponse response,
    HttpStatusCode statusCode, URI url, HttpMethod method) throws IOException {
    if (errorLoggerHandler != null) {
      try {
        errorLoggerHandler.handleError(url, method, response);
      } catch (Exception ignored) {
      }
    }

    if (statusCode.is4xxClientError()) {
      try (InputStream responseBodyStream = response.getBody()) {
        DebtPositionErrorDTO debtPositionErrorDTO = objectMapper.readValue(
          responseBodyStream, DebtPositionErrorDTO.class);
        transcodeDebtPositionsErrorAndThrow(debtPositionErrorDTO.getMessage());
      } catch (IOException ex) {
        log.error("Cannot read or deserialize DebtPositionError message", ex);
        throw ex;
      }
    } else {
      super.handleError(response, statusCode, url, method);
    }
  }

  private void transcodeDebtPositionsErrorAndThrow(
    String debtPositionsErrorMessage) {

    String nativeFaultCode = extractErrorCode(debtPositionsErrorMessage)
      .orElseThrow(() -> new SilFaultException(
        SilFaults.PAA_SYSTEM_ERROR,
        "Errore esterno con codice non trovato: "
          + debtPositionsErrorMessage
      ));

    SilFaults silFault;
    try {
      silFault = SilFaults.fromNativeFault2LegacyCode(nativeFaultCode);
    } catch (IllegalArgumentException e) {
      log.warn("Codice di errore non mappato: {}", nativeFaultCode);
      silFault = SilFaults.PAA_SYSTEM_ERROR;
    }

    throw new SilFaultException(silFault);
  }

  private Optional<String> extractErrorCode(String message) {
    if (message == null || message.isBlank()) {
      return Optional.empty();
    }

    Matcher matcher = ERROR_CODE_PATTERN.matcher(message.trim());

    if (matcher.matches() && matcher.groupCount() > 0) {
      return Optional.of(matcher.group(1));
    }

    return Optional.empty();
  }
}
