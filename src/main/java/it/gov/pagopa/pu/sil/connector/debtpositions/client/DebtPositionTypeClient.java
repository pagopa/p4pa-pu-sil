package it.gov.pagopa.pu.sil.connector.debtpositions.client;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionType;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApisHolder;
import it.gov.pagopa.pu.sil.exception.common.RestInvokeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DebtPositionTypeClient {

  private final DebtPositionsApisHolder debtPositionsApisHolder;

  public DebtPositionTypeClient(DebtPositionsApisHolder debtPositionsApisHolder) {
    this.debtPositionsApisHolder = debtPositionsApisHolder;
  }

  public DebtPositionTypeOrg getDebtPositionTypeOrgByOrganizationIdAndCode(Long organizationId, String debtPositionTypeOrgCode, String accessToken) {
    try {
      return debtPositionsApisHolder
        .getDebtPositionTypeOrgSearchControllerApi(accessToken)
        .crudDebtPositionTypeOrgsFindByOrganizationIdAndCode(organizationId, debtPositionTypeOrgCode);
    } catch (RestInvokeNotFoundException e) {
      log.info("Cannot find DeptPositionTypeOrg having orgId[{}] and code[{}]", organizationId, debtPositionTypeOrgCode, e);
      return null;
    }
  }

  public DebtPositionType getDebtPositionTypeById(Long debtPositionType, String accessToken) {
    try {
      return debtPositionsApisHolder
        .getDebtPositionTypeEntityControllerApi(accessToken)
        .crudGetDebtpositiontype(String.valueOf(debtPositionType));
    } catch (RestInvokeNotFoundException e) {
      log.info("Cannot find DeptPositionType having id[{}]", debtPositionType, e);
      return null;
    }
  }

  public DebtPositionTypeOrg getDebtPositionTypeOrgByInstallmentId(Long installmentId, String accessToken) {
    try {
      return debtPositionsApisHolder
        .getDebtPositionTypeOrgSearchControllerApi(accessToken)
        .crudDebtPositionTypeOrgsGetDebtPositionTypeOrgByInstallmentId(installmentId);
    } catch (RestInvokeNotFoundException e) {
      log.info("Cannot find DeptPositionTypeOrg for installmentId[{}]", installmentId, e);
      return null;
    }
  }

}
