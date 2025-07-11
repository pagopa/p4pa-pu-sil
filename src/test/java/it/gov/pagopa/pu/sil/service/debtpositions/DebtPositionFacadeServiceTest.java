package it.gov.pagopa.pu.sil.service.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebtPositionFacadeServiceTest {
  @Mock
  private DebtPositionService debtPositionService;

  @InjectMocks
  private DebtPositionFacadeService debtPositionFacadeService;

  @Test
  void testGetInstallmentsByOrganizationIdAndNav() {
    List<InstallmentDTO> expected = List.of(new InstallmentDTO());
    when(debtPositionService.getInstallmentsByOrganizationIdAndNav(anyLong(), anyString(), anyList(), anyString()))
      .thenReturn(expected);
    List<InstallmentDTO> result = debtPositionFacadeService.getInstallmentsByOrganizationIdAndNav(1L, "nav", "token");
    assertEquals(expected, result);
  }
}
