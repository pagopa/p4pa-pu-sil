package it.gov.pagopa.pu.sil.service.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionOrigin;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DebtPositionFacadeService {
  private static final List<DebtPositionOrigin> ALLOWED_ORIGINS = List.of(
    DebtPositionOrigin.ORDINARY,
    DebtPositionOrigin.ORDINARY_SIL,
    DebtPositionOrigin.SPONTANEOUS
  );

  private final DebtPositionService debtPositionService;

  public DebtPositionFacadeService(DebtPositionService debtPositionService) {
    this.debtPositionService = debtPositionService;
  }

  public List<InstallmentDTO> getInstallmentsByOrganizationIdAndNav(Long organizationId, String nav, String accessToken) {
    return debtPositionService.getInstallmentsByOrganizationIdAndNav(organizationId, nav, ALLOWED_ORIGINS, accessToken);
  }
}
