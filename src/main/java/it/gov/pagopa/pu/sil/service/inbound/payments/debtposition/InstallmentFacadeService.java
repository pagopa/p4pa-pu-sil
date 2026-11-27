package it.gov.pagopa.pu.sil.service.inbound.payments.debtposition;

import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.InstallmentService;
import it.gov.pagopa.pu.sil.util.Constants;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InstallmentFacadeService {

  private final InstallmentService installmentService;

  public InstallmentFacadeService(InstallmentService installmentService) {
    this.installmentService = installmentService;
  }

  public List<InstallmentDTO> getInstallmentsByOrganizationIdAndNav(Long organizationId, String nav, String accessToken) {
    return installmentService.getInstallmentsByOrganizationIdAndNav(organizationId, nav, Constants.ORDINARY_DEBT_POSITION_ORIGINS, accessToken)
      .stream()
      .filter(i -> i.getStatus() != InstallmentStatus.CANCELLED)
      .toList();
  }
}
