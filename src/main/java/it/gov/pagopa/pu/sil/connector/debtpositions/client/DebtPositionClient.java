package it.gov.pagopa.pu.sil.connector.debtpositions.client;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionType;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
@Slf4j
public class DebtPositionClient {

  private final DebtPositionsApisHolder debtPositionsApisHolder;

  public DebtPositionClient(DebtPositionsApisHolder debtPositionsApisHolder) {
    this.debtPositionsApisHolder = debtPositionsApisHolder;
  }

  public DebtPositionTypeOrg getDebtPositionTypeOrgByOrganizationIdAndCode(Long organizationId, String debtPositionTypeOrgCode, String accessToken) {
    try {
      return debtPositionsApisHolder
        .getDebtPositionTypeOrgSearchControllerApi(accessToken)
        .crudDebtPositionTypeOrgsFindByOrganizationIdAndCode(organizationId, debtPositionTypeOrgCode);
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Cannot find DeptPositionTypeOrg having orgId[{}] and code[{}]", organizationId, debtPositionTypeOrgCode, e);
      return null;
    }
  }

  public DebtPositionType getDebtPositionTypeById(Long debtPositionType, String accessToken) {
    try {
      return debtPositionsApisHolder
        .getDebtPositionTypeEntityControllerApi(accessToken)
        .crudGetDebtpositiontype(String.valueOf(debtPositionType));
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Cannot find DeptPositionType having id[{}]", debtPositionType, e);
      return null;
    }
  }

  public Long countExistingInstallmentsByIudIuvNav(Long organizationId, String iud, String iuv, String nav, String accessToken) {
    return debtPositionsApisHolder
      .getInstallmentNoPiiSearchControllerApi(accessToken)
      .crudInstallmentsCountExistingInstallments(organizationId, iud, iuv, nav);
  }

}
