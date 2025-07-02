package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.PaymentOptionDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SessionIdMapperTest {

  @InjectMocks
  private SessionIdMapper sessionIdMapper;

  @Test
  void mapDebtPositionsToSessionIdReturnsCorrectSessionId() {
    DebtPositionDTO debtPosition = new DebtPositionDTO();
    PaymentOptionDTO paymentOption = new PaymentOptionDTO();
    InstallmentDTO installment1 = new InstallmentDTO();
    installment1.setInstallmentId(1L);
    InstallmentDTO installment2 = new InstallmentDTO();
    installment2.setInstallmentId(2L);
    paymentOption.setInstallments(List.of(installment1, installment2));
    debtPosition.setPaymentOptions(List.of(paymentOption));

    String sessionId = sessionIdMapper.mapDebtPositionsToSessionId(List.of(debtPosition));

    assertEquals(String.join(Constants.SESSION_ID_SEPARATOR, "1","2"),  sessionId);
  }

  @Test
  void mapDebtPositionsToSessionIdHandlesEmptyDebtPositions() {
    String sessionId = sessionIdMapper.mapDebtPositionsToSessionId(List.of());

    assertEquals("", sessionId);
  }

  @Test
  void mapSessionIdToInstallmentIdsReturnsCorrectInstallmentIds() {
    List<Long> installmentIds = sessionIdMapper.mapSessionIdToInstallmentIds(
      String.join(Constants.SESSION_ID_SEPARATOR, "1","2","3")
    );

    assertEquals(List.of(1L, 2L, 3L), installmentIds);
  }

  @Test
  void mapSessionIdToInstallmentIdsThrowsExceptionForInvalidSessionId() {
    String ids = String.join(Constants.SESSION_ID_SEPARATOR, "invalid","session", "id");
    SilFaultException exception = assertThrows(SilFaultException.class, () ->
      sessionIdMapper.mapSessionIdToInstallmentIds(ids));

    assertEquals(SilFaults.PAA_ID_SESSION_NON_VALIDO, exception.getFault());
    assertEquals("ID session non valido", exception.getDescription());
  }

  @Test
  void mapSessionIdToInstallmentIdsHandlesEmptySessionId() {
    SilFaultException exception = assertThrows(SilFaultException.class, () ->
      sessionIdMapper.mapSessionIdToInstallmentIds("")
    );

    assertEquals(SilFaults.PAA_ID_SESSION_NON_VALIDO, exception.getFault());
    assertEquals("ID session non valido", exception.getDescription());
  }
}
