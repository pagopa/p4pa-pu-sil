package it.gov.pagopa.pu.sil.enums.legacy;

import it.gov.pagopa.pu.sil.dto.generated.ExportStatusResponseDTO;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ExportFileLegacyStatus {
  REQUESTED(ExportStatusResponseDTO.StatusEnum.REQUESTED, "LOAD_EXPORT"),
  PROCESSING(ExportStatusResponseDTO.StatusEnum.PROCESSING, "EXPORT_IN_ELAB"),
  COMPLETED(ExportStatusResponseDTO.StatusEnum.COMPLETED, "EXPORT_ESEGUITO"),
  COMPLETED_NO_DATA_FOUND(ExportStatusResponseDTO.StatusEnum.COMPLETED_NO_DATA_FOUND, "EXPORT_ESEGUITO_NESSUN_DOVUTO_TROVATO"),
  EXPIRED(ExportStatusResponseDTO.StatusEnum.EXPIRED, "EXPORT_CANCELLATO"),
  ERROR(ExportStatusResponseDTO.StatusEnum.ERROR, "ERROR_EXPORT")
  ;

  private final ExportStatusResponseDTO.StatusEnum value;
  private final String legacyValue;

  ExportFileLegacyStatus(ExportStatusResponseDTO.StatusEnum value, String legacyValue) {
    this.value = value;
    this.legacyValue = legacyValue;
  }

  public static String fromValue2LegacyValue(ExportStatusResponseDTO.StatusEnum value) {
    return Arrays.stream(ExportFileLegacyStatus.values())
      .filter(e -> e.getValue().equals(value))
      .findFirst()
      .map(ExportFileLegacyStatus::getLegacyValue)
      .orElseThrow(()-> new IllegalArgumentException("Unexpected value %s".formatted(value)));
  }
}
