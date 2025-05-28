package it.gov.pagopa.pu.sil.util.soap;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.util.Utilities;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;
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
}
