package it.gov.pagopa.pu.sil.util.soap;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.veneto.regione.pagamenti.ente.FaultBean;
import it.veneto.regione.pagamenti.ente.PaaSILChiediAvvisiPendentiRisposta;
import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class FaultUtilsTest {
  @Test
  void testSetFaultOnResponse() {
      // Arrange
    PaaSILChiediAvvisiPendentiRisposta responseObj = new PaaSILChiediAvvisiPendentiRisposta();
    FaultBean faultBean = new FaultBean();
    Supplier<Object> faultBeanSupplier = () -> faultBean;
    BiConsumer<Object, Object> faultSetter = (response, bean) -> {
      assertInstanceOf(FaultBean.class, bean);
    };

    // Act
    Object result = FaultUtils.setFaultOnResponse(
      responseObj,
      SilFaults.PAA_SYSTEM_ERROR,
      "Fault String 1",
      faultBeanSupplier,
      faultSetter
    );

    // Assert
    assertEquals(responseObj, result);
    assertEquals("Fault String 1", faultBean.getDescription());
    assertNotNull(faultBean.getId());
    assertEquals(SilFaults.PAA_SYSTEM_ERROR.code(), faultBean.getFaultCode());
    assertEquals(SilFaults.PAA_SYSTEM_ERROR.description(), faultBean.getFaultString());
    assertEquals(0, faultBean.getSerial());
  }
}
