package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface DebtPositionService {
  DebtPositionTypeOrg getDebtPositionTypeOrgByOrgIdAndType(Long organizationId, String debtPositionTypeOrgCode, String accessToken);
  DebtPositionType getDebtPositionTypeById(Long debtPositionType, String accessToken);
  Long countExistingInstallmentsByIudIuvNav(Long organizationId, String iud, String iuv, String nav, String accessToken);
  /**
   * Creates a new debt position.
   *
   * @param debtPositionDTO the DTO containing the details of the debt position to create
   * @param accessToken the access token for authentication
   * @return a pair containing the created DebtPositionDTO and the workflow ID from the response headers
   */
  Pair<DebtPositionDTO, String> createDebtPosition(DebtPositionDTO debtPositionDTO, String accessToken);

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
}
