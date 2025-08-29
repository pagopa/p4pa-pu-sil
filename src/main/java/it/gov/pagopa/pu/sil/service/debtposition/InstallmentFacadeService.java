package it.gov.pagopa.pu.sil.service.debtposition;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionOrigin;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.InstallmentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InstallmentFacadeService {
  public static final List<DebtPositionOrigin> ALLOWED_ORIGINS = List.of(
    DebtPositionOrigin.ORDINARY,
    DebtPositionOrigin.ORDINARY_SIL,
    DebtPositionOrigin.SPONTANEOUS,
    DebtPositionOrigin.SPONTANEOUS_SIL
  );

  private final InstallmentService installmentService;

  public InstallmentFacadeService(InstallmentService installmentService) {
    this.installmentService = installmentService;
  }

  public List<InstallmentDTO> getInstallmentsByOrganizationIdAndNav(Long organizationId, String nav, String accessToken) {
    return installmentService.getInstallmentsByOrganizationIdAndNav(organizationId, nav, ALLOWED_ORIGINS, accessToken)
      .stream()
      .filter(i -> i.getStatus() != InstallmentStatus.CANCELLED)
      .toList();
  }
}
