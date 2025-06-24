package it.gov.pagopa.pu.sil.util;

import java.time.ZoneId;

public class Constants {

  private Constants(){}

  public static final ZoneId ZONEID = ZoneId.of("Europe/Rome");

  public static final String AUX_DIGIT = "3";

  public static final String STAMP_DEBT_POSITION_TYPE_ORG_CODE = "STAMP";

  public static final long EXPIRATION_SPONTANEOUS_SIL_DEBT_POSITION_MINUTES = 120L;

  public static final String ANONYMOUS_FISCAL_CODE = "ANONIMO";

  public static final String SOURCE_FLOW_NAME_PREFIX_INVIADOVUTI = "SIL_ID-";

  public static final String SOURCE_FLOW_NAME_PREFIX_INVIACARRELLODOVUTI = "SIL_ICD-";

  public static final int MAX_CART_SIZE = 5;

  public static final String WORKFLOW_STATUS_COMPLETED_VALUE = "WORKFLOW_EXECUTION_STATUS_COMPLETED";

  public static final String SESSION_ID_SEPARATOR = "-";
}

