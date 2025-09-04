package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionOrigin;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentNoPII;
import it.gov.pagopa.pu.sil.connector.debtpositions.client.InstallmentClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class InstallmentServiceImpl implements InstallmentService {

  private final InstallmentClient client;

  public InstallmentServiceImpl(InstallmentClient client) {
    this.client = client;
  }

  public Boolean isInstallmentExistsByIudIuvNav(Long organizationId, String iud, String iuv, String nav, List<DebtPositionOrigin> debtPositionOrigins, String accessToken) {
    return client.isInstallmentExistsByIudIuvNav(organizationId, iud, iuv, nav, debtPositionOrigins, accessToken);
  }

  @Override
  public List<InstallmentDTO> getInstallmentsByOrganizationIdAndNav(Long organizationId, String nav, List<DebtPositionOrigin> debtPositionOrigins, String accessToken) {
    return client.getInstallmentsByOrganizationIdAndNav(organizationId, nav, debtPositionOrigins, accessToken);
  }

  @Override
  public InstallmentNoPII findAuthorizedByTransferSemanticKey(Long organizationId, String iuv, String iur, int transferIndex, String operatorExternalUserId, List<DebtPositionOrigin> debtPositionOrigins, String accessToken) {
    return client.findAuthorizedByTransferSemanticKey(
      organizationId, iuv, iur, transferIndex, operatorExternalUserId, debtPositionOrigins, accessToken);
  }
}
