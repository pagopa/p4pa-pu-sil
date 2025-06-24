package it.gov.pagopa.pu.sil.service.debtposition;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.workflow.service.WorkflowService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static it.gov.pagopa.pu.sil.util.Constants.WORKFLOW_STATUS_COMPLETED_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateDebtPositionServiceTest {

  @InjectMocks
  private CreateDebtPositionService createDebtPositionService;

  @Mock
  private DebtPositionService debtPositionServiceMock;
  @Mock
  private WorkflowService workflowServiceMock;

  @Test
  void testCreateSyncedDebtPositions_AllSyncedSuccessfully() {
    // Arrange
    DebtPositionDTO debtPosition1 = new DebtPositionDTO();
    debtPosition1.setDebtPositionId(1L);
    DebtPositionDTO debtPosition2 = new DebtPositionDTO();
    debtPosition2.setDebtPositionId(2L);

    when(debtPositionServiceMock.createDebtPosition(any(DebtPositionDTO.class), anyString()))
      .thenReturn(Pair.of(debtPosition1, "workflow1"))
      .thenReturn(Pair.of(debtPosition2, "workflow2"));

    when(workflowServiceMock.waitWorkflowCompletion(anyString(), anyInt(), anyInt(), anyString()))
      .thenReturn(WORKFLOW_STATUS_COMPLETED_VALUE);

    // Act
    List<DebtPositionDTO> result = createDebtPositionService.createSyncedDebtPositions(
      List.of(debtPosition1, debtPosition2), "accessToken");

    // Assert
    assertEquals(2, result.size());
  }

  @Test
  void testCreateSyncedDebtPositions_SomeFailedToSync() {
    // Arrange
    DebtPositionDTO debtPosition1 = new DebtPositionDTO();
    debtPosition1.setDebtPositionId(1L);
    DebtPositionDTO debtPosition2 = new DebtPositionDTO();
    debtPosition2.setDebtPositionId(2L);

    when(debtPositionServiceMock.createDebtPosition(any(DebtPositionDTO.class), anyString()))
      .thenReturn(Pair.of(debtPosition1, "workflow1"))
      .thenReturn(Pair.of(debtPosition2, "workflow2"));

    when(workflowServiceMock.waitWorkflowCompletion(eq("workflow1"), anyInt(), anyInt(), anyString()))
      .thenReturn(WORKFLOW_STATUS_COMPLETED_VALUE);
    when(workflowServiceMock.waitWorkflowCompletion(eq("workflow2"), anyInt(), anyInt(), anyString()))
      .thenReturn("FAILED");

    // Act
    List<DebtPositionDTO> debtPositionDTOList = List.of(debtPosition1, debtPosition2);
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> createDebtPositionService.createSyncedDebtPositions(
      debtPositionDTOList, "accessToken"));

    // Assert
    assertEquals(SilFaults.PAA_SYSTEM_ERROR, exception.getFault());
    assertEquals("errore sincronizzando le posizioni debitorie", exception.getDescription());
  }
}
