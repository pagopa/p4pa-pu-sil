package it.gov.pagopa.pu.sil.connector.debtpositions.client;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApisHolder;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
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
    try{
      return debtPositionsApisHolder
        .getDebtPositionTypeOrgSearchControllerApi(accessToken)
        .crudDebtPositionTypeOrgsFindByOrganizationIdAndCode(organizationId, debtPositionTypeOrgCode);
    } catch (HttpClientErrorException.NotFound e){
      log.info("Cannot find DeptPositionTypeOrg having orgId[{}] and code[{}]", organizationId, debtPositionTypeOrgCode, e);
      return null;
    }
  }

}
