package it.gov.pagopa.pu.sil.service.inbound.payments.debtposition;

import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.InstallmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstallmentFacadeServiceTest {
  @Mock
  private InstallmentService installmentServiceMock;

  @InjectMocks
  private InstallmentFacadeService installmentFacadeService;

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      installmentServiceMock
    );
  }

  @Test
  void testGetInstallmentsByOrganizationIdAndNav() {
    List<InstallmentDTO> expected = List.of(new InstallmentDTO());
    when(installmentServiceMock.getInstallmentsByOrganizationIdAndNav(anyLong(), anyString(), anyList(), anyString()))
      .thenReturn(expected);
    List<InstallmentDTO> result = installmentFacadeService.getInstallmentsByOrganizationIdAndNav(1L, "nav", "token");
    assertEquals(expected, result);
  }
}
