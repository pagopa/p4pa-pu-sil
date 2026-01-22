package it.gov.pagopa.pu.sil.util;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionOrigin;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionStatus;

import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;

public class Constants {

  private Constants(){}

  public static final ZoneId ZONEID = ZoneId.of("Europe/Rome");
  public static final TimeZone DEFAULT_TIMEZONE = TimeZone.getTimeZone(ZONEID);

  public static final String AUX_DIGIT = "3";

  public static final long EXPIRATION_SPONTANEOUS_SIL_DEBT_POSITION_MINUTES = 120L;

  public static final String ANONYMOUS_FISCAL_CODE = "ANONIMO";

  public static final String SOURCE_FLOW_NAME_PREFIX_INVIADOVUTI = "SIL_ID-";

  public static final String SOURCE_FLOW_NAME_PREFIX_INVIACARRELLODOVUTI = "SIL_ICD-";

  public static final String MIXED_DP_TYPE_ORG_CODE = "MIXED";

  public static final List<String> EXCLUDED_DEBT_POSITION_TYPE_CODES = List.of(MIXED_DP_TYPE_ORG_CODE);

  public static final int MAX_CART_SIZE = 5;

  public static final String WORKFLOW_STATUS_COMPLETED_VALUE = "WORKFLOW_EXECUTION_STATUS_COMPLETED";

  public static final String SESSION_ID_SEPARATOR = "-";

  public static final String LEGACY_PAYMENT_OUTCOME_CODE_OK = "0";

  public static final String LEGACY_IMPORT_ACTION_INSERT = "I";
  public static final String LEGACY_IMPORT_ACTION_MODIFY = "M";
  public static final String LEGACY_IMPORT_ACTION_CANCEL = "A";
  public static final String LEGACY_IMPORT_ACTION_PRINT = "S";

  public static final String INSTALLMENT_REMITTANCE_INFORMATION_PLACEHOLDER = "Pagamento on-the-fly";

  public static final List<DebtPositionOrigin> PRINT_NOTICE_ALLOWED_ORIGINS = List.of(
    DebtPositionOrigin.ORDINARY,
    DebtPositionOrigin.ORDINARY_SIL,
    DebtPositionOrigin.SPONTANEOUS
  );
  public static final List<DebtPositionOrigin> ORDINARY_DEBT_POSITION_ORIGINS = List.of(
    DebtPositionOrigin.ORDINARY,
    DebtPositionOrigin.ORDINARY_SIL,
    DebtPositionOrigin.SPONTANEOUS,
    DebtPositionOrigin.SPONTANEOUS_SIL
  );

  public static final List<DebtPositionStatus> SYNCABLE_DEBT_POSITION_STATUSES = List.of(
    DebtPositionStatus.DRAFT,
    DebtPositionStatus.UNPAID,
    DebtPositionStatus.PARTIALLY_PAID,
    DebtPositionStatus.EXPIRED
  );
}

