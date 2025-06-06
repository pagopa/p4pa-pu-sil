package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.sil.config.CacheConfig;
import it.gov.pagopa.pu.sil.connector.debtpositions.client.DebtPositionClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DebtPositionServiceImpl implements DebtPositionService {

  private final DebtPositionClient client;

  public DebtPositionServiceImpl(DebtPositionClient client) {
    this.client = client;
  }

  @Cacheable(cacheNames = CacheConfig.Fields.debtPositionTypeOrg, key = "#organizationId + '-' + #debtPositionTypeOrgCode", unless="#result == null")
  public DebtPositionTypeOrg getDebtPositionTypeOrgByOrgIdAndType(Long organizationId, String debtPositionTypeOrgCode, String accessToken) {
    return client.getDebtPositionTypeOrgByOrganizationIdAndCode(organizationId, debtPositionTypeOrgCode, accessToken);
  }

}
