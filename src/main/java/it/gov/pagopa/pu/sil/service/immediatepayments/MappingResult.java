package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.MixedDebtPositionDTO;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record MappingResult(List<DebtPositionDTO> debtPositions, List<MixedDebtPositionDTO> mixedDebtPositions) {

  public static MappingResult ofDebtPositions(List<DebtPositionDTO> debtPositions) {
    Objects.requireNonNull(debtPositions, "debtPositions must not be null");
    return new MappingResult(debtPositions, Collections.emptyList());
  }

  public static MappingResult ofMixedDebtPositions(List<MixedDebtPositionDTO> mixedDebtPositions) {
    Objects.requireNonNull(mixedDebtPositions, "mixedDebtPositions must not be null");
    return new MappingResult(Collections.emptyList(), mixedDebtPositions);
  }

  public boolean isMixed() {
    return mixedDebtPositions != null && !mixedDebtPositions.isEmpty();
  }
}
