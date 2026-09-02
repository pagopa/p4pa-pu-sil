package it.gov.pagopa.pu.sil.connector.workflow.client;

import it.gov.pagopa.pu.sil.connector.workflow.config.WorkflowApisHolder;
import it.gov.pagopa.pu.sil.exception.common.BaseBusinessException;
import it.gov.pagopa.pu.sil.exception.common.RestInvokeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WorkflowApiClient {

  private final WorkflowApisHolder workflowApisHolder;

  public WorkflowApiClient(WorkflowApisHolder workflowApisHolder) {
    this.workflowApisHolder = workflowApisHolder;
  }


  public String waitWorkflowCompletion(String workflowId, Integer maxAttempts, Integer retryDelayMs, String accessToken) {
    try {
      return workflowApisHolder.getWorkflowApi(accessToken).waitWorkflowCompletion(workflowId, maxAttempts, retryDelayMs).getStatus();
    } catch (BaseBusinessException e) {
      if(e instanceof RestInvokeException) {
        return e.getCode();
      }
      throw e;
    }
  }
}
