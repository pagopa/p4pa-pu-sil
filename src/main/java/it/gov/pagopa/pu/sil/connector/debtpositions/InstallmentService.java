package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionOrigin;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentNoPII;

import java.util.List;

public interface InstallmentService {
  Long countExistingInstallmentsByIudIuvNav(Long organizationId, String iud, String iuv, String nav, String accessToken);
                                                                 /**
   * Retrieves a list of InstallmentDTOs based on organization ID, NAV, and debt position origins.
   *
   * @param organizationId the ID of the organization
   * @param nav the NAV (Navigation) identifier
   * @param debtPositionOrigin the list of debt position origins to filter by
   * @param accessToken the access token for authentication
   * @return a list of InstallmentDTOs matching the criteria
   */
  List<InstallmentDTO> getInstallmentsByOrganizationIdAndNav(Long organizationId, String nav, List<DebtPositionOrigin> debtPositionOrigin, String accessToken);


  /**
   * Finds an InstallmentNoPII by transfer semantic key.
   *
   * @param organizationId the ID of the organization
   * @param iuv the IUV (Identificativo Unico di Versamento)
   * @param iur the IUR (Identificativo Unico di Riscossione)
   * @param transferIndex the index of the transfer
   * @param operatorExternalUserId the external user ID of the operator
   * @param accessToken the access token for authentication
   * @return an InstallmentNoPII if found, otherwise null
   */
  InstallmentNoPII findAuthorizedByTransferSemanticKey(
    Long organizationId,
    String iuv,
    String iur,
    int transferIndex,
    String operatorExternalUserId,
    String accessToken);
}
