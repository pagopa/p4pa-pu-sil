package it.gov.pagopa.pu.sil.util.soap;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.IngestionFlowFileTypeValidationException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.veneto.regione.pagamenti.ente.FaultBean;
import it.veneto.regione.pagamenti.ente.Risposta;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class FaultUtils {
  private FaultUtils() {}

  public static <T, F> T setFaultOnResponse(
      T responseObj,
      SilFaults fault,
      String description,
      Supplier<F> faultBeanSupplier,
      BiConsumer<T, F> faultSetter
  ) {
    F faultBean = faultBeanSupplier.get();
    if (faultBean instanceof it.veneto.regione.pagamenti.ente.FaultBean fb) {
      fb.setFaultCode(fault.code());
      fb.setDescription(description);
      fb.setFaultString(fault.description());
      fb.setId(Utilities.getTraceId());
      fb.setSerial(Utilities.systemTimeSecondsFrom2025());
    } else if (faultBean instanceof it.veneto.regione.pagamenti.pivot.ente.FaultBean fb) {
      fb.setFaultCode(fault.code());
      fb.setDescription(description);
      fb.setFaultString(fault.description());
      fb.setId(Utilities.getTraceId());
      fb.setSerial(Utilities.systemTimeSecondsFrom2025());
    }
    faultSetter.accept(responseObj, faultBean);
    return responseObj;
  }

  public static <T extends Risposta> T setFaultOnResponse(T response, SilFaults fault, String description) {
    return FaultUtils.setFaultOnResponse(response,
      fault,
      description,
      FaultBean::new,
      T::setFault
    );
  }

  public static <T extends it.veneto.regione.pagamenti.pivot.ente.Risposta> T setFaultOnResponse(T response, SilFaults fault, String description) {
    return FaultUtils.setFaultOnResponse(response,
      fault,
      description,
      it.veneto.regione.pagamenti.pivot.ente.FaultBean::new,
      T::setFault
    );
  }

  public static <T, F> T systemErrorFaultResponse(
      T response,
      Exception e,
      SilFaults fault,
      String description,
      Supplier<F> faultBeanSupplier,
      BiConsumer<T, F> faultSetter) {
    log.error("System error occurred", e);
    return setFaultOnResponse(response, fault, description, faultBeanSupplier, faultSetter);
  }

  public static <T, F> Function<Exception, T> unauthorizedExceptionHandler(
    Supplier<T> responseSupplier,
    BiConsumer<T, F> faultSetter,
    Supplier<F> faultBeanSupplier,
    SilFaults unauthorizedFault,
    SilFaults systemErrorFault) {
    return unauthorizedExceptionHandler(responseSupplier.get(), faultSetter, faultBeanSupplier, unauthorizedFault, systemErrorFault);
  }

  public static <T, F> Function<Exception, T> unauthorizedExceptionHandler(
      T responseObj,
      BiConsumer<T, F> faultSetter,
      Supplier<F> faultBeanSupplier,
      SilFaults unauthorizedFault,
      SilFaults systemErrorFault) {
    return (Exception e) -> {
      if (e instanceof UnauthorizedException ue) {
        return setFaultOnResponse(
          responseObj,
          unauthorizedFault,
          ue.getMessage(),
          faultBeanSupplier,
          faultSetter
        );
      }
      return systemErrorFaultResponse(
        responseObj,
        e,
        systemErrorFault,
        "Errore di sistema",
        faultBeanSupplier,
        faultSetter
      );
    };
  }
}
