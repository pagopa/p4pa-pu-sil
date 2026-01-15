package it.gov.pagopa.pu.sil.connector.debtpositions.client;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionOrigin;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentNoPII;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

@Service
@Slf4j
public class InstallmentClient {

  private final DebtPositionsApisHolder debtPositionsApisHolder;

  public InstallmentClient(DebtPositionsApisHolder debtPositionsApisHolder) {
    this.debtPositionsApisHolder = debtPositionsApisHolder;
  }

  public Boolean isInstallmentExistsByIudIuvNav(Long organizationId, String iud, String iuv, String nav, List<DebtPositionOrigin> debtPositionOrigins, String accessToken) {
    return debtPositionsApisHolder
      .getInstallmentNoPiiSearchControllerApi(accessToken)
      .crudInstallmentsIsInstallmentExists(organizationId, iud, iuv, nav, debtPositionOrigins);
  }

  public List<InstallmentDTO> getInstallmentsByOrganizationIdAndNav(Long organizationId, String nav, List<DebtPositionOrigin> debtPositionOrigin, String accessToken) {
    return debtPositionsApisHolder
      .getInstallmentApi(accessToken)
      .getInstallmentsByOrganizationIdAndNav(organizationId, nav, debtPositionOrigin);
  }

  public InstallmentNoPII findAuthorizedByTransferSemanticKey(
      Long organizationId,
      String iuv,
      String iur,
      int transferIndex,
      String operatorExternalUserId,
      List<DebtPositionOrigin> debtPositionOrigins,
      String accessToken) {
    try {
      return debtPositionsApisHolder
        .getInstallmentNoPiiSearchControllerApi(accessToken)
        .crudInstallmentsFindAuthorizedByTransferSemanticKey(organizationId, iuv, iur, String.valueOf(transferIndex), operatorExternalUserId, debtPositionOrigins);
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Cannot find InstallmentNoPII by semantic key: organizationId {} - iuv {} - iur {} - transferIndex {}", organizationId, iuv, iur, transferIndex, e);
      return null;
    }
  }
}
