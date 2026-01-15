package it.gov.pagopa.pu.sil.connector.debtpositions.client;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionType;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
@Slf4j
public class DebtPositionTypeClient {

  private final DebtPositionsApisHolder debtPositionsApisHolder;

  public DebtPositionTypeClient(DebtPositionsApisHolder debtPositionsApisHolder) {
    this.debtPositionsApisHolder = debtPositionsApisHolder;
  }

  public DebtPositionTypeOrg getDebtPositionTypeOrgByOrganizationIdAndCode(Long organizationId, String debtPositionTypeOrgCode, String accessToken) {
    return debtPositionsApisHolder
      .getDebtPositionTypeOrgSearchControllerApi(accessToken)
      .crudDebtPositionTypeOrgsFindByOrganizationIdAndCode(organizationId, debtPositionTypeOrgCode);
  }

  public DebtPositionType getDebtPositionTypeById(Long debtPositionType, String accessToken) {
    return debtPositionsApisHolder
      .getDebtPositionTypeEntityControllerApi(accessToken)
      .crudGetDebtpositiontype(String.valueOf(debtPositionType));
  }

  public DebtPositionTypeOrg getDebtPositionTypeOrgByInstallmentId(Long installmentId, String accessToken) {
    return debtPositionsApisHolder
      .getDebtPositionTypeOrgSearchControllerApi(accessToken)
      .crudDebtPositionTypeOrgsGetDebtPositionTypeOrgByInstallmentId(installmentId);
  }

}
