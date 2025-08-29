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

  @Override
  public Long countExistingInstallmentsByIudIuvNav(Long organizationId, String iud, String iuv, String nav, String accessToken) {
    return client.countExistingInstallmentsByIudIuvNav(organizationId, iud, iuv, nav, accessToken);
  }

  @Override
  public List<InstallmentDTO> getInstallmentsByOrganizationIdAndNav(Long organizationId, String nav, List<DebtPositionOrigin> debtPositionOrigin, String accessToken) {
    return client.getInstallmentsByOrganizationIdAndNav(organizationId, nav, debtPositionOrigin, accessToken);
  }

  @Override
  public InstallmentNoPII findAuthorizedByTransferSemanticKey(Long organizationId, String iuv, String iur, int transferIndex, String operatorExternalUserId, String accessToken) {
    return client.findAuthorizedByTransferSemanticKey(
      organizationId, iuv, iur, transferIndex, operatorExternalUserId, accessToken);
  }
}
