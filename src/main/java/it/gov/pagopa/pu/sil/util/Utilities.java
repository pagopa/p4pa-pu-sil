package it.gov.pagopa.pu.sil.util;

import org.slf4j.MDC;

public class Utilities {

  private Utilities() {
  }

  public static String getTraceId(){
    return MDC.get("traceId");
  }
}
