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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpServerErrorException;

@ExtendWith(MockitoExtension.class)
class DebtPositionsResponseErrorHandlerTest {
  private static final String BASE_URL = "http://example.com";
  private static final URI DUMMY_URI = URI.create(BASE_URL);
  public static final HttpStatusCode STATUS_400 = HttpStatusCode.valueOf(400);

  private DebtPositionsResponseErrorHandler errorHandler;

  @Mock
  private ClientHttpResponse responseMock;

  private final ObjectMapper objectMapper = new ObjectMapper();

  void setUp(boolean isPrintBodyWhenError) throws IOException {
    DebtPositionsApiClientConfig clientConfig = DebtPositionsApiClientConfig.builder()
      .baseUrl(BASE_URL)
      .printBodyWhenError(isPrintBodyWhenError)
      .build();
    errorHandler = new DebtPositionsResponseErrorHandler(clientConfig, objectMapper);

    if (isPrintBodyWhenError) {
      when(responseMock.getStatusCode()).thenReturn(STATUS_400);
    }
    when(responseMock.getStatusText()).thenReturn("STATUS_TEXT");
    when(responseMock.getHeaders()).thenReturn(HttpHeaders.EMPTY);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void handleError_shouldTranscodeAndThrowSilFaultExceptionForMappedCode(boolean isPrintBodyWhenError)
    throws IOException {
    setUp(isPrintBodyWhenError);

    String jsonBody = objectMapper.writeValueAsString(new DebtPositionErrorDTO()
      .message("[P4PA_INVALID_IUV] The iuv must be 17 characters long"));

    when(responseMock.getBody()).thenReturn(new ByteArrayInputStream(jsonBody.getBytes(StandardCharsets.UTF_8)));

    Executable exec = () -> errorHandler.handleError(responseMock, STATUS_400, DUMMY_URI, HttpMethod.POST);
    SilFaultException exception = assertThrows(SilFaultException.class, exec);
    assertEquals(SilFaults.PAA_IUV_NON_VALIDO, exception.getFault());
  }

  @Test
  void handleError_shouldTranscodeAndThrowPaaSystemSilFaultExceptionForNonMappedCode()
    throws IOException {
    setUp(false);

    String jsonBody = objectMapper.writeValueAsString(new DebtPositionErrorDTO()
      .message("[NOT_MAPPED] Not mapped error message."));

    when(responseMock.getBody()).thenReturn(new ByteArrayInputStream(jsonBody.getBytes(StandardCharsets.UTF_8)));

    Executable exec = () -> errorHandler.handleError(responseMock, STATUS_400, DUMMY_URI, HttpMethod.POST);
    SilFaultException exception = assertThrows(SilFaultException.class, exec);
    assertEquals(SilFaults.PAA_SYSTEM_ERROR, exception.getFault());
  }

  @Test
  void handleError_shouldThrowHttpServerExceptionWhenStatus5xx()
    throws IOException {
    setUp(false);

    Executable exec = () -> errorHandler.handleError(responseMock, HttpStatusCode.valueOf(500), DUMMY_URI, HttpMethod.POST);
    assertThrows(HttpServerErrorException.class, exec);
  }

  @Test
  void handleError_shouldThrowPaaSystemExceptionWhenCannotDeserializeDebtPositionError()
    throws IOException {
    setUp(false);

    when(responseMock.getBody()).thenReturn(new ByteArrayInputStream("MALFORMED_DEBT_POSITION_ERROR".getBytes(StandardCharsets.UTF_8)));

    Executable exec = () -> errorHandler.handleError(responseMock, STATUS_400, DUMMY_URI, HttpMethod.POST);
    assertThrows(IOException.class, exec);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "ERROR", "[MALFORMED"})
  void extractErrorCode_shouldThrowPaaSystemExceptionWhenCodeIsNotValid(String debtPositionsErrorMessage)
    throws IOException {
    setUp(false);

    String jsonBody = objectMapper.writeValueAsString(new DebtPositionErrorDTO()
      .message(debtPositionsErrorMessage));

    when(responseMock.getBody()).thenReturn(new ByteArrayInputStream(jsonBody.getBytes(StandardCharsets.UTF_8)));

    Executable exec = () -> errorHandler.handleError(responseMock, STATUS_400, DUMMY_URI, HttpMethod.POST);
    SilFaultException exception = assertThrows(SilFaultException.class, exec);

    assertEquals(SilFaults.PAA_SYSTEM_ERROR, exception.getFault());
    assertTrue(exception.getDescription()
      .contains("Errore esterno con codice non trovato"));
  }

}
