package it.gov.pagopa.pu.sil.enums.legacy;

import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFileStatus;

import java.util.Arrays;

public enum ExportFileLegacyStatus {
  REQUESTED(ExportFileStatus.REQUESTED, "LOAD_EXPORT"),
  PROCESSING(ExportFileStatus.PROCESSING, "EXPORT_IN_ELAB"),
  COMPLETED(ExportFileStatus.COMPLETED, "EXPORT_ESEGUITO"),
  EXPIRED(ExportFileStatus.EXPIRED, "EXPORT_CANCELLATO"),
  ERROR(ExportFileStatus.ERROR, "ERROR_EXPORT")
  ;

  private ExportFileStatus value;
  private String legacyValue;

  ExportFileLegacyStatus(ExportFileStatus value, String legacyValue) {
    this.value = value;
    this.legacyValue = legacyValue;
  }

  public ExportFileStatus getValue() { return value; }
  public String getLegacyValue() { return legacyValue; }

  public static String fromValue2LegacyValue(ExportFileStatus value) {
    return Arrays.stream(ExportFileLegacyStatus.values())
      .filter(e -> e.getValue().equals(value))
      .findFirst()
      .map(ExportFileLegacyStatus::getLegacyValue)
      .orElseThrow(()-> new IllegalArgumentException("Unexpected value %s".formatted(value)));
  }
}
