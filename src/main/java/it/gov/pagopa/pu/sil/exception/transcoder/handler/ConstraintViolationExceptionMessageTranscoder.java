package it.gov.pagopa.pu.sil.exception.transcoder.handler;

import it.gov.pagopa.pu.sil.dto.generated.ErrorFieldDTO;
import it.gov.pagopa.pu.sil.dto.generated.PuSilErrorDTO;
import it.gov.pagopa.pu.sil.exception.transcoder.ExceptionMessageTranscoded;
import it.gov.pagopa.pu.sil.exception.transcoder.ExceptionMessageTranscoder;
import jakarta.validation.ConstraintViolationException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ConstraintViolationExceptionMessageTranscoder implements ExceptionMessageTranscoder<ConstraintViolationException> {
  @Override
  public ExceptionMessageTranscoded transcode(ConstraintViolationException constraintViolationException) {
    List<ErrorFieldDTO> errorFields = constraintViolationException.getConstraintViolations()
      .stream()
      .map(e -> (ErrorFieldDTO)ErrorFieldDTO.builder()
        .field(e.getPropertyPath().toString())
        .error(e.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
        .message(e.getMessage())
        .build()
      )
      .sorted(Comparator.comparing(ErrorFieldDTO::getField))
      .toList();

    String errorDescription = errorFields.stream()
      .map(e -> " " + e.getField() + ": " + e.getMessage())
      .collect(Collectors.joining(";"));

    return new ExceptionMessageTranscoded(
      PuSilErrorDTO.CategoryEnum.BAD_REQUEST.name(),
      "Invalid request content." + errorDescription,
      errorFields
    );
  }
}
