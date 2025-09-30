package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.sil.service.debtposition.ManageDebtPositionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InstantPaymentsFacade {
  private final ManageDebtPositionService manageDebtPositionService;

  public InstantPaymentsFacade(ManageDebtPositionService manageDebtPositionService) {
    this.manageDebtPositionService = manageDebtPositionService;
  }

  public List<DebtPositionDTO> createDebtPositionsFromMapping(MappingResult mappingResult, String accessToken) {
    if (mappingResult.isMixed()) {
      return manageDebtPositionService.createMixedDebtPositions(mappingResult.mixedDebtPositions(), accessToken);
    } else {
      return manageDebtPositionService.createDebtPositions(mappingResult.debtPositions(), accessToken);
    }
  }
}
