package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;

public interface DebtPositionService {
  DebtPositionTypeOrg getDebtPositionTypeOrgByOrgIdAndType(Long organizationId, String debtPositionTypeOrgCode, String accessToken);
}
