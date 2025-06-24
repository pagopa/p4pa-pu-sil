package it.gov.pagopa.pu.sil.service.debtposition;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.workflow.service.WorkflowService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreateDebtPositionService {

  private final DebtPositionService debtPositionService;
  private final WorkflowService workflowService;

  public List<DebtPositionDTO> createSyncedDebtPositions(List<DebtPositionDTO> debtPositionsToCreate, String accessToken) {
    //create debt positions
    List<Pair<DebtPositionDTO, String>> debtPositions = debtPositionsToCreate.stream()
      .map(dp -> debtPositionService.createDebtPosition(dp, accessToken))
      .toList();

    Map<String, String> mdcContextMap = MDC.getCopyOfContextMap();
    //wait for the debt positions to be synced
    List<Triple<DebtPositionDTO, String, String>> debtPositionsNotSync = debtPositions.stream().parallel()
      .map(dp -> {
        MDC.setContextMap(mdcContextMap);
        log.debug("Waiting for workflow completion for debt position [{}] with workflowId: [{}]", dp.getLeft().getDebtPositionId(), dp.getRight());
        String result = workflowService.waitWorkflowCompletion(dp.getRight(), 10, 1000, accessToken);
        log.info("Workflow completed for debt position [{}] with workflowId: [{}] with result: [{}]",
          dp.getLeft().getDebtPositionId(), dp.getRight(), result);
        return Triple.of(dp.getLeft(), dp.getRight(), result);
      })
      .filter(triple -> !Constants.WORKFLOW_STATUS_COMPLETED_VALUE.equals(triple.getRight()))
      .toList();

    //if any of the debt positions failed to sync, return a fault response
    if (!debtPositionsNotSync.isEmpty()) {
      log.error("error syncing debt positions: {}",
        debtPositionsNotSync.stream()
          .map(triple -> String.format("DebtPositionId: %s, WorkflowId: %s, Result: %s",
            triple.getLeft().getDebtPositionId(), triple.getMiddle(), triple.getRight()))
          .collect(Collectors.joining(" ; ")));
      throw new SilFaultException(SilFaults.PAA_SYSTEM_ERROR, "errore sincronizzando le posizioni debitorie");
    }

    return debtPositions.stream()
      .map(Pair::getLeft)
      .toList();
  }
}
