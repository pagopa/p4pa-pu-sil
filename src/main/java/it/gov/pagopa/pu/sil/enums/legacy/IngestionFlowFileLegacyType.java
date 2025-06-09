package it.gov.pagopa.pu.sil.enums.legacy;

import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.sil.exception.IngestionFlowFileTypeValidationException;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public enum IngestionFlowFileLegacyType {
  TREASURY_OPI(IngestionFlowFileTypeEnum.TREASURY_OPI, "O"),
  TREASURY_CSV(IngestionFlowFileTypeEnum.TREASURY_CSV, "C"),
  TREASURY_XLS(IngestionFlowFileTypeEnum.TREASURY_XLS, "T"),
  TREASURY_POSTE(IngestionFlowFileTypeEnum.TREASURY_POSTE, "Y"),
  ;

  private final IngestionFlowFileTypeEnum value;
  private final String legacyValue;

  IngestionFlowFileLegacyType(IngestionFlowFileTypeEnum value,
                              String legacyValue) {
    this.value = value;
    this.legacyValue = legacyValue;
  }

  public IngestionFlowFileTypeEnum getValue() { return this.value; }

  public String getLegacyValue() { return legacyValue; }

  public static IngestionFlowFileTypeEnum fromLegacyValue2CurrentValue(String legacyValue) {
    return Arrays.stream(IngestionFlowFileLegacyType.values())
      .filter(e -> e.getLegacyValue().equals(legacyValue))
      .findFirst()
      .map(IngestionFlowFileLegacyType::getValue)
      .orElseThrow(() -> {
        log.error("Invalid ingestion flow file type: {}", legacyValue);
        throw new IngestionFlowFileTypeValidationException("Tipo di flusso non valido: %s".formatted(legacyValue));
      });
  }
}
