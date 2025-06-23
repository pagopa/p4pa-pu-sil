package it.gov.pagopa.pu.sil.connector.workflow.service;

import it.gov.pagopa.pu.debtpositions.connector.workflow.client.WorkflowApiClient;
import it.gov.pagopa.pu.debtpositions.dto.WfExecutionParameters;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.workflowhub.dto.generated.PaymentEventType;
import it.gov.pagopa.pu.workflowhub.dto.generated.WorkflowCreatedDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

  @Mock
  private WorkflowApiClient workflowApiClientMock;

  private WorkflowService workflowService;

  @BeforeEach
  void init() {
    workflowService = new WorkflowServiceImpl(
      workflowApiClientMock
    );
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      workflowApiClientMock
    );
  }

  @Test
  void whenSyncDebtPositionThenOk() {
    // Given
    String accessToken = "ACCESSTOKEN";
    DebtPositionDTO debtPositionDTO = new DebtPositionDTO();
    WfExecutionParameters wfExecutionParameters = new WfExecutionParameters();
    PaymentEventType paymentEventType = PaymentEventType.DP_CREATED;
    String eventDescription = "EVENTDESCRIPTION";
    WorkflowCreatedDTO expectedResult = new WorkflowCreatedDTO("1", "runId");

    Mockito.when(workflowApiClientMock.syncDebtPosition(Mockito.same(debtPositionDTO), Mockito.same(wfExecutionParameters), Mockito.same(paymentEventType), Mockito.same(eventDescription), Mockito.same(accessToken)))
      .thenReturn(expectedResult);

    // When
    WorkflowCreatedDTO result = workflowService.syncDebtPosition(debtPositionDTO, wfExecutionParameters, paymentEventType, eventDescription, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }

}
