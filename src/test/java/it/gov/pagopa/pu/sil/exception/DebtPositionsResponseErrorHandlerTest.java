package it.gov.pagopa.pu.sil.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionErrorDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApiClientConfig;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

@ExtendWith(MockitoExtension.class)
class DebtPositionsResponseErrorHandlerTest {

  private static final String BASE_URL = "http://example.com";
  private static final URI DUMMY_URI = URI.create(BASE_URL);

  private DebtPositionsResponseErrorHandler errorHandler;

  @Mock
  private ResponseErrorHandler errorLoggerHandlerMock;
  @Mock
  private ClientHttpResponse responseMock;

  private ObjectMapper objectMapper;

  void setUp(boolean isPrintBodyWhenError) {
    DebtPositionsApiClientConfig clientConfig = DebtPositionsApiClientConfig.builder()
      .baseUrl(BASE_URL)
      .printBodyWhenError(isPrintBodyWhenError)
      .build();
    objectMapper = new ObjectMapper();
    errorHandler = new DebtPositionsResponseErrorHandler(clientConfig,
      objectMapper);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      errorLoggerHandlerMock
    );
  }

  @Test
  void handleError_shouldTranscodeAndThrowSilFaultExceptionForMappedCode()
    throws IOException {
    setUp(true);

    String jsonBody = objectMapper.writeValueAsString(new DebtPositionErrorDTO()
      .message("[P4PA_INVALID_IUV] The iuv must be 17 characters long"));
    when(responseMock.getBody())
      .thenReturn(  // First return because errorLoggerHandler will consume the stream
        new ByteArrayInputStream(jsonBody.getBytes(StandardCharsets.UTF_8)))
      .thenReturn(
        new ByteArrayInputStream(jsonBody.getBytes(StandardCharsets.UTF_8)));

    SilFaultException exception = assertThrows(SilFaultException.class, () ->
      errorHandler.handleError(responseMock, HttpStatusCode.valueOf(400),
        DUMMY_URI,
        HttpMethod.POST)
    );
    assertEquals(SilFaults.PAA_IUV_NON_VALIDO, exception.getFault());
  }

  @Test
  void handleError_shouldFallbackToPaaSystemErrorForUnmappedNativeCode()
    throws IOException {
    setUp(false);

    String jsonBody = objectMapper.writeValueAsString(new DebtPositionErrorDTO()
      .message("[UNKNOWN_CODE] This is an unmapped error message"));
    when(responseMock.getBody()).thenReturn(
      new ByteArrayInputStream(jsonBody.getBytes(StandardCharsets.UTF_8)));

    SilFaultException exception = assertThrows(SilFaultException.class, () ->
      errorHandler.handleError(responseMock, HttpStatusCode.valueOf(400),
        DUMMY_URI, HttpMethod.POST)
    );

    assertEquals(SilFaults.PAA_SYSTEM_ERROR, exception.getFault());
  }

  @Test
  void handleError_shouldThrowExceptionIfCannotDeserializeResponseBody()
    throws IOException {
    setUp(false);

    when(responseMock.getBody()).thenThrow(IOException.class);

    assertThrows(IOException.class, () ->
      errorHandler.handleError(responseMock, HttpStatusCode.valueOf(400),
        DUMMY_URI,
        HttpMethod.POST)
    );
  }

  @Test
  void handleError_shouldDelegateToSuperHandleErrorFor5xxStatus() {
    setUp(false);

    assertThrows(Exception.class,
      () -> errorHandler.handleError(responseMock, HttpStatusCode.valueOf(500),
        DUMMY_URI, HttpMethod.POST));
  }

  @Test
  void extractErrorCode_shouldThrowPaaSystemExceptionWhenOriginalMessageIsBlank()
    throws IOException {
    setUp(false);

    String jsonBody = objectMapper.writeValueAsString(new DebtPositionErrorDTO()
      .message(""));
    when(responseMock.getBody()).thenReturn(
      new ByteArrayInputStream(jsonBody.getBytes(StandardCharsets.UTF_8)));

    SilFaultException exception = assertThrows(SilFaultException.class, () ->
      errorHandler.handleError(responseMock, HttpStatusCode.valueOf(400),
        DUMMY_URI, HttpMethod.POST)
    );

    assertEquals(SilFaults.PAA_SYSTEM_ERROR, exception.getFault());
    assertTrue(exception.getDescription()
      .contains("Errore esterno con codice non trovato"));
  }

}
