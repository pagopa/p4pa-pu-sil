package it.gov.pagopa.pu.sil.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionErrorDTO;
import it.gov.pagopa.pu.sil.config.rest.RestTemplateConfig;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApiClientConfig;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResponseErrorHandler;

@Slf4j
public class DebtPositionsResponseErrorHandler extends
  DefaultResponseErrorHandler {

  private static final String DEBT_POSITIONS = "DEBT-POSITIONS";

  private final ObjectMapper objectMapper;
  private final ResponseErrorHandler errorLoggerHandler;

  public DebtPositionsResponseErrorHandler(DebtPositionsApiClientConfig clientConfig,
    ObjectMapper objectMapper) {
    this.errorLoggerHandler = clientConfig.isPrintBodyWhenError() ?
      RestTemplateConfig.bodyPrinterWhenError(DEBT_POSITIONS)
      : null;
    this.objectMapper = objectMapper;
  }

  @Override
  public void handleError(ClientHttpResponse response,
    HttpStatusCode statusCode, URI url, HttpMethod method) throws IOException {
    try {
      if (errorLoggerHandler != null) {
        errorLoggerHandler.handleError(url, method, response);
      } else {
        super.handleError(response, statusCode, url, method);
      }
    } catch (HttpStatusCodeException exception) {
      if (statusCode.is4xxClientError()) {
        String responseBody = exception.getResponseBodyAsString();
        DebtPositionErrorDTO debtPositionErrorDTO;

        try {
          debtPositionErrorDTO = objectMapper.readValue(responseBody, DebtPositionErrorDTO.class);
        } catch (IOException ex) {
          log.error("Cannot deserialize DebtPositionError message from body: {}", responseBody, ex);
          throw ex;
        }

        transcodeDebtPositionsErrorAndThrow(debtPositionErrorDTO.getMessage());
      } else {
        throw exception;
      }
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

    String trimmedMessage = message.trim();
    if (!trimmedMessage.startsWith("[")) {
      return Optional.empty();
    }

    int closingBracketIndex = trimmedMessage.indexOf(']');
    if (closingBracketIndex > 1) {
      String code = trimmedMessage.substring(1, closingBracketIndex);

      if (!code.isBlank()) {
        return Optional.of(code);
      }
    }

    return Optional.empty();
  }
}
