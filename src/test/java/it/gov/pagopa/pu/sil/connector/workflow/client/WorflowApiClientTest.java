package it.gov.pagopa.pu.sil.connector.workflow.client;

import it.gov.pagopa.pu.debtpositions.connector.workflow.config.WorkflowApisHolder;
import it.gov.pagopa.pu.debtpositions.dto.WfExecutionParameters;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.workflowhub.controller.generated.DebtPositionApi;
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
class WorflowApiClientTest {
  @Mock
  private WorkflowApisHolder workflowApisHolderMock;
  @Mock
  private DebtPositionApi debtPositionApiMock;

  private WorkflowApiClient workflowApiClient;

  @BeforeEach
  void setUp() {
    workflowApiClient = new WorkflowApiClient(workflowApisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      workflowApisHolderMock
    );
  }

  @Test
  void whenHandleDpSyncThenInvokeWithAccessToken() {
    // Given
    String accessToken = "ACCESSTOKEN";
    DebtPositionDTO debtPositionDTO = new DebtPositionDTO();
    Boolean massive = Boolean.TRUE;
    Boolean partialChange = Boolean.FALSE;
    WfExecutionParameters wfExecutionParameters = WfExecutionParameters.builder()
      .massive(massive)
      .partialChange(partialChange)
      .build();
    PaymentEventType paymentEventType = PaymentEventType.DP_CREATED;
    String eventDescription = "EVENTDESCRIPTION";
    WorkflowCreatedDTO expectedResult = new WorkflowCreatedDTO("1", "runId");

    Mockito.when(workflowApisHolderMock.getDebtPositionApi(accessToken))
      .thenReturn(debtPositionApiMock);
    Mockito.when(debtPositionApiMock.syncDebtPosition(Mockito.argThat(i -> i.getDebtPosition() == debtPositionDTO && i.getExecutionConfig() == null), Mockito.same(massive), Mockito.same(partialChange), Mockito.same(paymentEventType), Mockito.same(eventDescription)))
      .thenReturn(new WorkflowCreatedDTO("1", "runId"));

    // When
    WorkflowCreatedDTO result = workflowApiClient.syncDebtPosition(debtPositionDTO, wfExecutionParameters, paymentEventType, eventDescription, accessToken);

    // Then
    Assertions.assertSame(expectedResult.getWorkflowId(), result.getWorkflowId());
  }

}
