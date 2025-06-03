package it.gov.pagopa.pu.sil.util.soap;

import it.gov.pagopa.pu.sil.UtilitiesTest;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.veneto.regione.pagamenti.ente.FaultBean;
import it.veneto.regione.pagamenti.ente.PaaSILChiediAvvisiPendentiRisposta;
import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class FaultUtilsTest {
  @Test
  void givenEnteFaultWhenSetFaultOnResponseThenOk() {
    // Arrange
    PaaSILChiediAvvisiPendentiRisposta responseObj = new PaaSILChiediAvvisiPendentiRisposta();
    FaultBean faultBean = new FaultBean();
    Supplier<Object> faultBeanSupplier = () -> faultBean;
    BiConsumer<Object, Object> faultSetter = (response, bean) -> {
      assertInstanceOf(FaultBean.class, bean);
    };
    UtilitiesTest.setTraceId("TRACE_ID");

    // Act
    Object result = FaultUtils.setFaultOnResponse(
      responseObj,
      SilFaults.PAA_SYSTEM_ERROR,
      "Fault String 1",
      faultBeanSupplier,
      faultSetter
    );

    UtilitiesTest.clearTraceIdContext();

    // Assert
    assertEquals(responseObj, result);
    assertEquals("Fault String 1", faultBean.getDescription());
    assertEquals("TRACE_ID", faultBean.getId());
    assertEquals(SilFaults.PAA_SYSTEM_ERROR.code(), faultBean.getFaultCode());
    assertEquals(SilFaults.PAA_SYSTEM_ERROR.description(), faultBean.getFaultString());
    assertNotNull(faultBean.getSerial());
  }

  @Test
  void givenPivotFaultWhenSetFaultOnResponseThenOk() {
    // Arrange
    PaaSILChiediAvvisiPendentiRisposta responseObj = new PaaSILChiediAvvisiPendentiRisposta();
    it.veneto.regione.pagamenti.pivot.ente.FaultBean faultBean = new it.veneto.regione.pagamenti.pivot.ente.FaultBean();
    Supplier<Object> faultBeanSupplier = () -> faultBean;
    BiConsumer<Object, Object> faultSetter = (response, bean) -> {
      assertInstanceOf(it.veneto.regione.pagamenti.pivot.ente.FaultBean.class, bean);
    };
    UtilitiesTest.setTraceId("TRACE_ID");

    // Act
    Object result = FaultUtils.setFaultOnResponse(
      responseObj,
      SilFaults.PIVOT_SYSTEM_ERROR,
      "Fault String 1",
      faultBeanSupplier,
      faultSetter
    );

    UtilitiesTest.clearTraceIdContext();

    // Assert
    assertEquals(responseObj, result);
    assertEquals("Fault String 1", faultBean.getDescription());
    assertEquals("TRACE_ID", faultBean.getId());
    assertEquals(SilFaults.PIVOT_SYSTEM_ERROR.code(), faultBean.getFaultCode());
    assertEquals(SilFaults.PIVOT_SYSTEM_ERROR.description(), faultBean.getFaultString());
    assertNotNull(faultBean.getSerial());
  }

  @Test
  void whenSystemErrorFaultResponseThenReturnFault() {
    PaaSILChiediAvvisiPendentiRisposta responseObj = new PaaSILChiediAvvisiPendentiRisposta();
    FaultBean faultBean = new FaultBean();
    Supplier<FaultBean> faultBeanSupplier = () -> faultBean;
    BiConsumer<PaaSILChiediAvvisiPendentiRisposta, FaultBean> faultSetter = PaaSILChiediAvvisiPendentiRisposta::setFault;
    UtilitiesTest.setTraceId("TRACE_ID");

    Object result = FaultUtils.systemErrorFaultResponse(
      responseObj,
      new RuntimeException("system error"),
      SilFaults.PAA_SYSTEM_ERROR,
      "desc",
      faultBeanSupplier,
      faultSetter
    );

    UtilitiesTest.clearTraceIdContext();

    assertEquals(responseObj, result);
    assertEquals("desc", faultBean.getDescription());
    assertEquals("TRACE_ID", faultBean.getId());
    assertEquals(SilFaults.PAA_SYSTEM_ERROR.code(), faultBean.getFaultCode());
    assertEquals(SilFaults.PAA_SYSTEM_ERROR.description(), faultBean.getFaultString());
    assertNotNull(faultBean.getSerial());
  }
}

