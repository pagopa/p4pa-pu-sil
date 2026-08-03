package it.gov.pagopa.pu.sil.config.rest;

import it.gov.pagopa.pu.debtpositions.dto.generated.CategoryEnum;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionErrorDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.ErrorFieldDTO;
import it.gov.pagopa.pu.sil.config.json.JsonConfig;
import it.gov.pagopa.pu.sil.connector.debtpositions.mapper.DebtPositionErrorDTOMapper;
import it.gov.pagopa.pu.sil.exception.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

class HttpClientErrorJsonBodyHandlerTest {

  private final JsonMapper jsonMapper = new JsonConfig().objectMapperJackson3();

  HttpClientErrorJsonBodyHandlerTest() throws URISyntaxException {
  }

  private HttpClientErrorJsonBodyHandler<DebtPositionErrorDTO> buildHttpClientErrorHandler(boolean bodyPrinterWhenError) {
    return new HttpClientErrorJsonBodyHandler<>(jsonMapper, "APPNAME", bodyPrinterWhenError,
      DebtPositionErrorDTO.class, DebtPositionErrorDTOMapper::map);
  }

  private final URI url = new URI("http://www.sample.com");
  private final DebtPositionErrorDTO expectedErrorDTO = new DebtPositionErrorDTO(CategoryEnum.DEBT_POSITION_BAD_REQUEST, "BADREQUEST", "MESSAGE", List.of(new ErrorFieldDTO("FIELD", "FIELDERRORCODE", "FIELDERRORMESSAGE")), "TRACEID");

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testNo4xxException(boolean bodyPrinterWhenError) {
    // Given
    HttpClientErrorJsonBodyHandler<DebtPositionErrorDTO> httpClientHandler = buildHttpClientErrorHandler(bodyPrinterWhenError);
    try (MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.SERVICE_UNAVAILABLE)) {

      // When
      HttpServerErrorException.ServiceUnavailable result = Assertions.assertThrows(HttpServerErrorException.ServiceUnavailable.class, () -> httpClientHandler.handleError(url, HttpMethod.GET, response));

      // Then
      Assertions.assertEquals("503 Service Unavailable on GET request for \"http://www.sample.com\": [no body]", result.getMessage());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testNoBodyException(boolean bodyPrinterWhenError) {
    // Given
    HttpClientErrorJsonBodyHandler<DebtPositionErrorDTO> httpClientHandler = buildHttpClientErrorHandler(bodyPrinterWhenError);
    try (MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.BAD_REQUEST)) {

      // When
      HttpClientErrorException.BadRequest result = Assertions.assertThrows(HttpClientErrorException.BadRequest.class, () -> httpClientHandler.handleError(url, HttpMethod.GET, response));

      // Then
      Assertions.assertEquals("400 Bad Request on GET request for \"http://www.sample.com\": [no body]", result.getMessage());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testNotFoundException(boolean bodyPrinterWhenError) {
    // Given
    HttpClientErrorJsonBodyHandler<DebtPositionErrorDTO> httpClientHandler = buildHttpClientErrorHandler(bodyPrinterWhenError);
    try (MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.NOT_FOUND)) {

      // When
      HttpClientErrorException.NotFound result = Assertions.assertThrows(HttpClientErrorException.NotFound.class, () -> httpClientHandler.handleError(url, HttpMethod.GET, response));

      // Then
      Assertions.assertEquals("404 Not Found on GET request for \"http://www.sample.com\": [no body]", result.getMessage());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testBodyException(boolean bodyPrinterWhenError) {
    // Given
    HttpClientErrorJsonBodyHandler<DebtPositionErrorDTO> httpClientHandler = buildHttpClientErrorHandler(bodyPrinterWhenError);
    try (MockClientHttpResponse response = new MockClientHttpResponse(jsonMapper.writeValueAsBytes(expectedErrorDTO), HttpStatus.BAD_REQUEST)) {

      // When
      RestInvokeInvalidValueException result = Assertions.assertThrows(RestInvokeInvalidValueException.class, () -> httpClientHandler.handleError(url, HttpMethod.GET, response));

      // Then
      Assertions.assertEquals("APPNAME", result.getApplicationName());
      Assertions.assertEquals(expectedErrorDTO.getCategory().getValue(), result.getCategory());
      Assertions.assertEquals(expectedErrorDTO.getCode(), result.getCode());
      Assertions.assertEquals(expectedErrorDTO.getMessage(), result.getMessage());
      Assertions.assertEquals(DebtPositionErrorDTOMapper.map(expectedErrorDTO).fields(), result.getFields());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testNoJsonBodyException(boolean bodyPrinterWhenError) {
    // Given
    HttpClientErrorJsonBodyHandler<DebtPositionErrorDTO> httpClientHandler = buildHttpClientErrorHandler(bodyPrinterWhenError);
    try (MockClientHttpResponse response = new MockClientHttpResponse("INVALIDJSON".getBytes(), HttpStatus.BAD_REQUEST)) {

      // When
      HttpClientErrorException.BadRequest result = Assertions.assertThrows(HttpClientErrorException.BadRequest.class, () -> httpClientHandler.handleError(url, HttpMethod.GET, response));

      // Then
      Assertions.assertEquals("400 Bad Request on GET request for \"http://www.sample.com\": \"INVALIDJSON\"", result.getMessage());
    }
  }


  private final Map<HttpStatus, Class<? extends BaseBusinessException>> httpStatus2ExpectedException = Map.of(
    HttpStatus.CONFLICT, RestInvokeConflictException.class,
    HttpStatus.FORBIDDEN, RestInvokeForbiddenException.class,
    HttpStatus.UNAUTHORIZED, RestInvokeNotAuthorizedException.class
  );

  @Test
  void testBuildDefaultHttpClientExceptionTranscoder(){
    BiFunction<HttpStatusCodeException, DebtPositionErrorDTO, RuntimeException> httpErrorTranscoder = HttpClientErrorJsonBodyHandler.buildDefaultHttpClientExceptionTranscoder("TEST", DebtPositionErrorDTO::getCode, DebtPositionErrorDTO::getMessage);
    DebtPositionErrorDTO errorDTO = new DebtPositionErrorDTO(null, "BAD_REQUEST", "MESSAGE", null, null);

    for (HttpStatus httpStatus : HttpStatus.values()) {
      RuntimeException result = httpErrorTranscoder
        .apply(new HttpClientErrorException(httpStatus), errorDTO);

      Assertions.assertInstanceOf(BaseBusinessException.class, result);
      Assertions.assertSame(errorDTO.getCode(), ((BaseBusinessException)result).getCode());
      Assertions.assertSame(errorDTO.getMessage(), result.getMessage());

      Class<? extends BaseBusinessException> expectedException = httpStatus2ExpectedException.getOrDefault(httpStatus, InvalidValueException.class);
      Assertions.assertInstanceOf(expectedException, result);
    }
  }

  @Test
  void testBuildDefaultHttpClientExceptionTranscoder_noErrorCodeFunction(){
    BiFunction<HttpStatusCodeException, DebtPositionErrorDTO, RuntimeException> httpErrorTranscoder = HttpClientErrorJsonBodyHandler.buildDefaultHttpClientExceptionTranscoder("TEST", null, DebtPositionErrorDTO::getMessage);
    DebtPositionErrorDTO errorDTO = new DebtPositionErrorDTO(null, "BAD_REQUEST", "MESSAGE", null, null);

    for (HttpStatus httpStatus : HttpStatus.values()) {
      RuntimeException result = httpErrorTranscoder
        .apply(new HttpClientErrorException(httpStatus), errorDTO);

      Assertions.assertInstanceOf(BaseBusinessException.class, result);
      Assertions.assertEquals(
        "TEST_" + httpStatus.name(),
        ((BaseBusinessException)result).getCode());
      Assertions.assertSame(errorDTO.getMessage(), result.getMessage());

      Class<? extends BaseBusinessException> expectedException = httpStatus2ExpectedException.getOrDefault(httpStatus, InvalidValueException.class);
      Assertions.assertInstanceOf(expectedException, result);
    }
  }
}
