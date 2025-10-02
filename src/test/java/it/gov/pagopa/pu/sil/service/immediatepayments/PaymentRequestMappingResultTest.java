package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.MixedDebtPositionDTO;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentRequestMappingResultTest {

  @Test
  void ofDebtPositions_shouldCreateWithDebtPositionsOnly() {
    DebtPositionDTO dto = new DebtPositionDTO();
    PaymentRequestMappingResult result = PaymentRequestMappingResult.ofDebtPositions(List.of(dto));

    assertEquals(1, result.debtPositions().size());
    assertTrue(result.mixedDebtPositions().isEmpty());
    assertFalse(result.isMixed());
  }

  @Test
  void ofMixedDebtPositions_shouldCreateWithMixedDebtPositionsOnly() {
    MixedDebtPositionDTO dto = new MixedDebtPositionDTO();
    PaymentRequestMappingResult result = PaymentRequestMappingResult.ofMixedDebtPositions(List.of(dto));

    assertEquals(1, result.mixedDebtPositions().size());
    assertTrue(result.debtPositions().isEmpty());
    assertTrue(result.isMixed());
  }

  @Test
  void isMixed_shouldReturnFalseWhenMixedDebtPositionsIsEmpty() {
    PaymentRequestMappingResult result = new PaymentRequestMappingResult(Collections.emptyList(), Collections.emptyList());
    assertFalse(result.isMixed());
  }

  @Test
  void ofDebtPositions_shouldThrowOnNull() {
    assertThrows(NullPointerException.class, () -> PaymentRequestMappingResult.ofDebtPositions(null));
  }

  @Test
  void ofMixedDebtPositions_shouldThrowOnNull() {
    assertThrows(NullPointerException.class, () -> PaymentRequestMappingResult.ofMixedDebtPositions(null));
  }
}
