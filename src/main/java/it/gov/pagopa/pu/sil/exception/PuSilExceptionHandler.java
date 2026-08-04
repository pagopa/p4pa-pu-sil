package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.dto.generated.PuSilErrorDTO;
import it.gov.pagopa.pu.sil.exception.common.CommonExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PuSilExceptionHandler extends CommonExceptionHandler {

  @ExceptionHandler({HttpClientErrorException.class})
  public ResponseEntity<PuSilErrorDTO> handleHttpClientErrorException(HttpClientErrorException ex, HttpServletRequest request) {
    return handleException(ex, request, ex.getStatusCode(), PuSilErrorDTO.CategoryEnum.GENERIC_ERROR);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<PuSilErrorDTO> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
    return handleException(ex, request, HttpStatus.NOT_FOUND, PuSilErrorDTO.CategoryEnum.BAD_REQUEST);
  }

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<PuSilErrorDTO> handlePaymentNotFoundException(PaymentNotFoundException ex, HttpServletRequest request) {
    return handleException(ex, request, HttpStatus.NOT_FOUND, PuSilErrorDTO.CategoryEnum.NOT_FOUND);
  }

  @ExceptionHandler(PaymentNotNotifiedException.class)
  public ResponseEntity<PuSilErrorDTO> handlePaymentNotNotifiedException(PaymentNotNotifiedException ex, HttpServletRequest request) {
    return handleException(ex, request, HttpStatus.PRECONDITION_FAILED, PuSilErrorDTO.CategoryEnum.BAD_REQUEST);
  }

  @ExceptionHandler(PaymentInvalidStatusException.class)
  public ResponseEntity<PuSilErrorDTO> handlePaymentInvalidStatusException(PaymentInvalidStatusException ex, HttpServletRequest request) {
    return handleException(ex, request, HttpStatus.CONFLICT, PuSilErrorDTO.CategoryEnum.BAD_REQUEST);
  }

  @ExceptionHandler(AssessmentNotFoundException.class)
  public ResponseEntity<PuSilErrorDTO> handleAssessmentNotFoundException(AssessmentNotFoundException ex, HttpServletRequest request) {
    return handleException(ex, request, HttpStatus.NOT_FOUND, PuSilErrorDTO.CategoryEnum.NOT_FOUND);
  }

  @ExceptionHandler(ExportFileClientException.class)
  public ResponseEntity<PuSilErrorDTO> handleExportFileClientException(ExportFileClientException ex, HttpServletRequest request) {
    return handleException(ex, request, HttpStatus.BAD_REQUEST, PuSilErrorDTO.CategoryEnum.BAD_REQUEST);
  }

  @ExceptionHandler(ExportFileServiceException.class)
  public ResponseEntity<PuSilErrorDTO> handleExportFileServiceException(ExportFileServiceException ex, HttpServletRequest request) {
    return handleException(ex, request, HttpStatus.BAD_REQUEST, PuSilErrorDTO.CategoryEnum.BAD_REQUEST);
  }

}
