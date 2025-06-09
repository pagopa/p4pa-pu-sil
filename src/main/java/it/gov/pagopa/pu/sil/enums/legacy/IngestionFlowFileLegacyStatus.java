package it.gov.pagopa.pu.sil.enums.legacy;

import java.util.Arrays;

public enum IngestionFlowFileLegacyStatus {
  UPLOADED("UPLOADED", "FILE_SCARICATO"),
  PROCESSING("PROCESSING", "FILE_IN_CARICAMENTO"),
  COMPLETED("COMPLETED", "FILE_CARICATO"),
  ERROR("ERROR", "ERROR_LOAD"),
  WAITING_FILE("WAITING_FILE", "WAITING_FILE") /** no such value exists in the legacy system */
  ;

  private String value;
  private String legacyValue;

  IngestionFlowFileLegacyStatus(String value, String legacyValue) {
    this.value = value;
    this.legacyValue = legacyValue;
  }

  public String getValue() { return value; }
  public String getLegacyValue() { return legacyValue; }

  public static String fromValue2LegacyValue(String value) {
    return Arrays.stream(IngestionFlowFileLegacyStatus.values())
      .filter(e -> e.getValue().equals(value))
      .findFirst()
      .map(IngestionFlowFileLegacyStatus::getLegacyValue)
      .orElseThrow(()-> new IllegalArgumentException("Unexpected value %s".formatted(value)));
  }
}
