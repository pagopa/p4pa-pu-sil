package it.gov.pagopa.pu.sil.util.soap;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Slf4j
public class FaultUtils {
  private FaultUtils() {}

  /**
   * Generic method to create and set a FaultBean on the response object.
   * @param responseObj the response object
   * @param fault the SilFaults
   * @param faultString the fault string
   * @param idFaultEmitter the emitter id
   * @param faultBeanSupplier a supplier to create the correct FaultBean type
   * @param faultSetter a setter to set the FaultBean on the response
   * @param <T> response type
   * @param <F> FaultBean type
   * @return responseObj with fault set
   */
  public static <T, F> T setFaultOnResponse(
      T responseObj,
      SilFaults fault,
      String faultString,
      String idFaultEmitter,
      Supplier<F> faultBeanSupplier,
      BiConsumer<T, F> faultSetter
  ) {
    F faultBean = faultBeanSupplier.get();
    if (faultBean instanceof it.veneto.regione.pagamenti.ente.FaultBean fb) {
      fb.setFaultCode(fault.code());
      fb.setDescription(fault.description());
      fb.setFaultString(faultString);
      fb.setId(idFaultEmitter);
      fb.setSerial(0);
    } else if (faultBean instanceof it.veneto.regione.pagamenti.pivot.ente.FaultBean fb) {
      fb.setFaultCode(fault.code());
      fb.setDescription(fault.description());
      fb.setFaultString(faultString);
      fb.setId(idFaultEmitter);
      fb.setSerial(0);
    }
    faultSetter.accept(responseObj, faultBean);
    return responseObj;
  }
}
