package it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.MixedDebtPositionDTO;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record PaymentRequestMappingResult(List<DebtPositionDTO> debtPositions, List<MixedDebtPositionDTO> mixedDebtPositions) {

  public static PaymentRequestMappingResult ofDebtPositions(List<DebtPositionDTO> debtPositions) {
    Objects.requireNonNull(debtPositions, "debtPositions must not be null");
    return new PaymentRequestMappingResult(debtPositions, Collections.emptyList());
  }

  public static PaymentRequestMappingResult ofMixedDebtPositions(List<MixedDebtPositionDTO> mixedDebtPositions) {
    Objects.requireNonNull(mixedDebtPositions, "mixedDebtPositions must not be null");
    return new PaymentRequestMappingResult(Collections.emptyList(), mixedDebtPositions);
  }

  public boolean isMixed() {
    return mixedDebtPositions != null && !mixedDebtPositions.isEmpty();
  }
}
