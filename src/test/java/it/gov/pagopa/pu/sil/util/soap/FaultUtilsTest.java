package it.gov.pagopa.pu.sil.util.soap;

import it.gov.pagopa.pu.sil.UtilitiesTest;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.veneto.regione.pagamenti.ente.FaultBean;
import it.veneto.regione.pagamenti.ente.PaaSILChiediAvvisiPendentiRisposta;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovutoRisposta;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILAutorizzaImportFlussoRisposta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

  static Stream<Object[]> unauthorizedOrSystemExceptionHandlerCases() {
    BiConsumer<PaaSILImportaDovutoRisposta, FaultBean> paaSILImportaDovutoFaultSetter = PaaSILImportaDovutoRisposta::setFault;
    Supplier<FaultBean> faultBeanSupplier = FaultBean::new;

    BiConsumer<PivotSILAutorizzaImportFlussoRisposta, it.veneto.regione.pagamenti.pivot.ente.FaultBean> pivotSILAutorizzaImportFlussoFaultSetter = PivotSILAutorizzaImportFlussoRisposta::setFault;
    Supplier<it.veneto.regione.pagamenti.pivot.ente.FaultBean> pivotFaultBeanSupplier = it.veneto.regione.pagamenti.pivot.ente.FaultBean::new;

    return Stream.of(
      new Object[] {
        new PaaSILImportaDovutoRisposta(),
        paaSILImportaDovutoFaultSetter,
        faultBeanSupplier,
        SilFaults.PAA_ENTE_NON_VALIDO,
        SilFaults.PAA_SYSTEM_ERROR,
        new UnauthorizedException("Not authorized"),
        SilFaults.PAA_ENTE_NON_VALIDO.code(),
        "Not authorized"
      },
      new Object[] {
        new PaaSILImportaDovutoRisposta(),
        paaSILImportaDovutoFaultSetter,
        faultBeanSupplier,
        SilFaults.PAA_ENTE_NON_VALIDO,
        SilFaults.PAA_SYSTEM_ERROR,
        new RuntimeException("Generic error"),
        SilFaults.PAA_SYSTEM_ERROR.code(),
        "Errore di sistema"
      },
      new Object[] {
        new PivotSILAutorizzaImportFlussoRisposta(),
        pivotSILAutorizzaImportFlussoFaultSetter,
        pivotFaultBeanSupplier,
        SilFaults.PIVOT_ENTE_NON_VALIDO,
        SilFaults.PIVOT_SYSTEM_ERROR,
        new UnauthorizedException("Pivot unauthorized"),
        SilFaults.PIVOT_ENTE_NON_VALIDO.code(),
        "Pivot unauthorized"
      },
      new Object[] {
        new PivotSILAutorizzaImportFlussoRisposta(),
        pivotSILAutorizzaImportFlussoFaultSetter,
        pivotFaultBeanSupplier,
        SilFaults.PIVOT_ENTE_NON_VALIDO,
        SilFaults.PIVOT_SYSTEM_ERROR,
        new RuntimeException("Pivot generic error"),
        SilFaults.PIVOT_SYSTEM_ERROR.code(),
        "Errore di sistema"
      }
    );
  }

  @ParameterizedTest
  @MethodSource("unauthorizedOrSystemExceptionHandlerCases")
  <T, F> void testUnauthorizedOrSystemExceptionHandler(
    T responseObj,
    BiConsumer<T, F> faultSetter,
    Supplier<F> faultBeanSupplier,
    SilFaults unauthorizedFault,
    SilFaults systemErrorFault,
    Exception exception,
    String expectedFaultCode,
    String expectedDescription
  ) {
    var handler = FaultUtils.unauthorizedOrSystemExceptionHandler(
      responseObj,
      faultSetter,
      faultBeanSupplier,
      unauthorizedFault,
      systemErrorFault
    );
    T resp = handler.apply(exception);
    Object fault = null;
    try {
      fault = resp.getClass().getMethod("getFault").invoke(resp);
    } catch (Exception e) {
      fail("Could not get fault from response: " + e.getMessage());
    }
    assertNotNull(fault);
    String faultCode = null;
    String description = null;
    try {
      faultCode = (String) fault.getClass().getMethod("getFaultCode").invoke(fault);
      description = (String) fault.getClass().getMethod("getDescription").invoke(fault);
    } catch (Exception e) {
      fail("Could not get faultCode/description: " + e.getMessage());
    }
    assertEquals(expectedFaultCode, faultCode);
    assertEquals(expectedDescription, description);
  }

}
