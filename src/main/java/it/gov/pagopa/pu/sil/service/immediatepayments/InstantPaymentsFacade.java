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

  public List<DebtPositionDTO> createDebtPositionsFromMapping(PaymentRequestMappingResult paymentRequestMappingResult, String accessToken) {
    if (paymentRequestMappingResult.isMixed()) {
      return manageDebtPositionService.createMixedDebtPositions(paymentRequestMappingResult.mixedDebtPositions(), accessToken);
    } else {
      return manageDebtPositionService.createDebtPositions(paymentRequestMappingResult.debtPositions(), accessToken);
    }
  }
}
