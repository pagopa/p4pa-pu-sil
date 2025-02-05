package it.gov.pagopa.pu.sil.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import it.gov.pagopa.pu.sil.dto.generated.PuSilErrorDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PuSilExceptionHandler {

  @ExceptionHandler({ValidationException.class, HttpMessageNotReadableException.class, MethodArgumentNotValidException.class})
  public ResponseEntity<PuSilErrorDTO> handleViolationException(Exception ex, HttpServletRequest request) {
    return handleException(ex, request, HttpStatus.BAD_REQUEST, PuSilErrorDTO.CodeEnum.BAD_REQUEST);
  }

  @ExceptionHandler({ServletException.class})
  public ResponseEntity<PuSilErrorDTO> handleServletException(ServletException ex, HttpServletRequest request) {
    HttpStatusCode httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    PuSilErrorDTO.CodeEnum errorCode = PuSilErrorDTO.CodeEnum.GENERIC_ERROR;
    if (ex instanceof ErrorResponse errorResponse) {
      httpStatus = errorResponse.getStatusCode();
      if (httpStatus.is4xxClientError()) {
        errorCode = PuSilErrorDTO.CodeEnum.BAD_REQUEST;
      }
    }
    return handleException(ex, request, httpStatus, errorCode);
  }

  @ExceptionHandler({RuntimeException.class})
  public ResponseEntity<PuSilErrorDTO> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
    return handleException(ex, request, HttpStatus.INTERNAL_SERVER_ERROR, PuSilErrorDTO.CodeEnum.GENERIC_ERROR);
  }

  static ResponseEntity<PuSilErrorDTO> handleException(Exception ex, HttpServletRequest request, HttpStatusCode httpStatus, PuSilErrorDTO.CodeEnum errorEnum) {
    logException(ex, request, httpStatus);

    String message = buildReturnedMessage(ex);

    return ResponseEntity
      .status(httpStatus)
      .body(new PuSilErrorDTO(errorEnum, message));
  }

  private static void logException(Exception ex, HttpServletRequest request, HttpStatusCode httpStatus) {
    log.info("A {} occurred handling request {}: HttpStatus {} - {}",
      ex.getClass(),
      getRequestDetails(request),
      httpStatus.value(),
      ex.getMessage());
  }

  private static String buildReturnedMessage(Exception ex) {
    if (ex instanceof HttpMessageNotReadableException) {
      if(ex.getCause() instanceof JsonMappingException jsonMappingException){
        return "Cannot parse body: " +
          jsonMappingException.getPath().stream()
            .map(JsonMappingException.Reference::getFieldName)
            .collect(Collectors.joining(".")) +
          ": " + jsonMappingException.getOriginalMessage();
      }
      return "Required request body is missing";
    } else if (ex instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
      return "Invalid request content:" +
        methodArgumentNotValidException.getBindingResult()
          .getAllErrors().stream()
          .map(e -> " " +
            (e instanceof FieldError fieldError? fieldError.getField(): e.getObjectName()) +
            ": " + e.getDefaultMessage())
          .sorted()
          .collect(Collectors.joining(";"));
    } else {
      return ex.getMessage();
    }
  }

  static String getRequestDetails(HttpServletRequest request) {
    return "%s %s".formatted(request.getMethod(), request.getRequestURI());
  }
}
