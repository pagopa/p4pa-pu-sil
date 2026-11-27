package it.gov.pagopa.pu.sil.exception.soap;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.soap.transcoder.BaseSoapExceptionTranscoder;
import it.gov.pagopa.pu.sil.util.Utilities;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;
import java.util.function.Function;

@Slf4j
public abstract class BaseSoapExceptionHandler<F> {

  private final BaseSoapExceptionTranscoder soapExceptionTranscoder;

  protected BaseSoapExceptionHandler(BaseSoapExceptionTranscoder soapExceptionTranscoder) {
    this.soapExceptionTranscoder = soapExceptionTranscoder;
  }

  public <R> Function<Exception, R> buildExceptionHandlerFunction(R response, BiConsumer<R, F> faultSetter) {
    return e -> setExceptionTranscoded(response, faultSetter, e);
  }

  public <R> R setExceptionTranscoded(R response, BiConsumer<R, F> faultSetter, Exception e) {
    faultSetter.accept(response, handleException(e));
    return response;
  }

  public <R> R setFault(R response, BiConsumer<R, F> faultSetter, SilFaults silFault, String message) {
    F fault = buildFault(new SoapFaultTranscoded(silFault, message));
    faultSetter.accept(response, fault);
    return response;
  }

  public F handleException(Exception exception) {
    SoapFaultTranscoded faultTranscoded = soapExceptionTranscoder.transcodeException(exception);

    return buildFault(faultTranscoded);
  }

  private F buildFault(SoapFaultTranscoded faultTranscoded) {
    String id = Utilities.getTraceId();
    int serial = Utilities.systemTimeSecondsFrom2025();
    return buildFault(id, faultTranscoded, serial);
  }

  protected abstract F buildFault(String id, SoapFaultTranscoded faultTranscoded, int serial);

}
