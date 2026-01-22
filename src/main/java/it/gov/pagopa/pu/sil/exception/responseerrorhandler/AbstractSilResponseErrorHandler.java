package it.gov.pagopa.pu.sil.exception.responseerrorhandler;

import it.gov.pagopa.pu.sil.config.rest.RestTemplateConfig;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResponseErrorHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@Slf4j
public abstract class AbstractSilResponseErrorHandler<E> extends DefaultResponseErrorHandler {
  private final JsonMapper jsonMapper;
  private final ResponseErrorHandler errorLoggerHandler;

  protected AbstractSilResponseErrorHandler(JsonMapper jsonMapper, boolean printBodyWhenError, String name) {
    this.jsonMapper = jsonMapper;
    this.errorLoggerHandler = printBodyWhenError ?
      RestTemplateConfig.bodyPrinterWhenError(name) : null;
  }

  protected abstract Class<E> getErrorDtoClass();
  protected abstract String extractMessageFromDto(E errorDto);

  @Override
  public void handleError(ClientHttpResponse response, HttpStatusCode statusCode, URI url, HttpMethod method) throws IOException {
    try {
      if (errorLoggerHandler != null) {
        errorLoggerHandler.handleError(url, method, response);
      } else {
        super.handleError(response, statusCode, url, method);
      }
    } catch (HttpStatusCodeException exception) {
      if (statusCode.is4xxClientError() && statusCode.value() != 404) {
        handleClientError(exception);
      } else {
        throw exception;
      }
    }
  }

  private void handleClientError(HttpStatusCodeException exception) throws IOException {
    String responseBody = exception.getResponseBodyAsString();
    E errorDto;

    try {
      errorDto = jsonMapper.readValue(responseBody, getErrorDtoClass());
    } catch (JacksonException ex) {
      log.error("Cannot deserialize error message from body: {}", responseBody, ex);
      throw new IOException("Error deserializing response body", ex);
    }

    String rawMessage = extractMessageFromDto(errorDto);
    transcodeErrorAndThrow(rawMessage);
  }

  private void transcodeErrorAndThrow(String errorMessage) {
    String nativeFaultCode = extractErrorCode(errorMessage)
      .orElseThrow(() -> new SilFaultException(
        SilFaults.PAA_SYSTEM_ERROR,
        "Errore esterno con codice non trovato: " + errorMessage
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
