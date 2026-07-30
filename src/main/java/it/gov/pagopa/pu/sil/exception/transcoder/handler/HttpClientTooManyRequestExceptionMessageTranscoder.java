package it.gov.pagopa.pu.sil.exception.transcoder.handler;

import it.gov.pagopa.pu.sil.dto.generated.PuSilErrorDTO;
import it.gov.pagopa.pu.sil.exception.transcoder.ExceptionMessageTranscoded;
import it.gov.pagopa.pu.sil.exception.transcoder.ExceptionMessageTranscoder;
import org.springframework.web.client.HttpClientErrorException;

public class HttpClientTooManyRequestExceptionMessageTranscoder implements ExceptionMessageTranscoder<HttpClientErrorException.TooManyRequests> {
  @Override
  public ExceptionMessageTranscoded transcode(HttpClientErrorException.TooManyRequests tooManyRequestsException) {
    return new ExceptionMessageTranscoded(
      PuSilErrorDTO.CategoryEnum.TOO_MANY_REQUESTS.name(),
      tooManyRequestsException.getMessage(),
      null);
  }
}
