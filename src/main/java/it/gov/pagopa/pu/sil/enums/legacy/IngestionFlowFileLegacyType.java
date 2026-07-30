package it.gov.pagopa.pu.sil.enums.legacy;

import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.sil.exception.IngestionFlowFileTypeValidationException;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Getter
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

  public static IngestionFlowFileTypeEnum fromLegacyValue2CurrentValue(String legacyValue) {
    return Arrays.stream(IngestionFlowFileLegacyType.values())
      .filter(e -> e.getLegacyValue().equals(legacyValue))
      .findFirst()
      .map(IngestionFlowFileLegacyType::getValue)
      .orElseThrow(() -> new IngestionFlowFileTypeValidationException(
        ErrorCodeConstants.ERROR_CODE_INVALID_INGESTION_FLOW_FILE_LEGACY_TYPE,
        "Invalid ingestion flow file type: %s".formatted(legacyValue),
        legacyValue));
  }
}
