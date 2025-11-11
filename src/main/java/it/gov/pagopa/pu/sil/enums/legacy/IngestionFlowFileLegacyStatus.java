package it.gov.pagopa.pu.sil.enums.legacy;

import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus;

import java.util.Arrays;

public enum IngestionFlowFileLegacyStatus {
  UPLOADED(IngestionFlowFileStatus.UPLOADED, "FILE_SCARICATO"),
  PROCESSING(IngestionFlowFileStatus.PROCESSING, "FILE_IN_CARICAMENTO"),
  COMPLETED(IngestionFlowFileStatus.COMPLETED, "FILE_CARICATO"),
  ERROR(IngestionFlowFileStatus.ERROR, "ERROR_LOAD"),
  WAITING_FILE(IngestionFlowFileStatus.WAITING_FILE, "WAITING_FILE"), // no such value exists in the legacy system
  WARNING(IngestionFlowFileStatus.WARNING, "WARNING"); // no such value exists in the legacy system

  private IngestionFlowFileStatus value;
  private String legacyValue;

  IngestionFlowFileLegacyStatus(IngestionFlowFileStatus value, String legacyValue) {
    this.value = value;
    this.legacyValue = legacyValue;
  }

  public IngestionFlowFileStatus getValue() { return value; }
  public String getLegacyValue() { return legacyValue; }

  public static String fromValue2LegacyValue(IngestionFlowFileStatus value) {
    return Arrays.stream(IngestionFlowFileLegacyStatus.values())
      .filter(e -> e.getValue().equals(value))
      .findFirst()
      .map(IngestionFlowFileLegacyStatus::getLegacyValue)
      .orElseThrow(()-> new IllegalArgumentException("Unexpected value %s".formatted(value)));
  }
}
