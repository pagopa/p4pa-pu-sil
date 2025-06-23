package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionType;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import org.apache.commons.lang3.tuple.Pair;

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
}
