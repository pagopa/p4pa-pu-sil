package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionType;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.sil.config.CacheConfig;
import it.gov.pagopa.pu.sil.connector.debtpositions.client.DebtPositionTypeClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DebtPositionTypeServiceImpl implements DebtPositionTypeService {

  private final DebtPositionTypeClient client;

  public DebtPositionTypeServiceImpl(DebtPositionTypeClient client) {
    this.client = client;
  }

  @Override
  @Cacheable(cacheNames = CacheConfig.Fields.debtPositionTypeOrg, key = "#organizationId + '-' + #debtPositionTypeOrgCode", unless="#result == null")
  public DebtPositionTypeOrg getDebtPositionTypeOrgByOrgIdAndType(Long organizationId, String debtPositionTypeOrgCode, String accessToken) {
    return client.getDebtPositionTypeOrgByOrganizationIdAndCode(organizationId, debtPositionTypeOrgCode, accessToken);
  }

  @Override
  @Cacheable(cacheNames = CacheConfig.Fields.debtPositionType, key = "#id", unless="#result == null")
  public DebtPositionType getDebtPositionTypeById(Long debtPositionType, String accessToken) {
    return client.getDebtPositionTypeById(debtPositionType, accessToken);
  }

  @Override
  public DebtPositionTypeOrg getDebtPositionTypeOrgByInstallmentId(Long installmentId, String accessToken) {
    return client.getDebtPositionTypeOrgByInstallmentId(installmentId, accessToken);
  }
}
