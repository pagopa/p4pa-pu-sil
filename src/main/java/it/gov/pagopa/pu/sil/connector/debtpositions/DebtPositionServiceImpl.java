package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.sil.config.CacheConfig;
import it.gov.pagopa.pu.sil.connector.debtpositions.client.DebtPositionClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DebtPositionServiceImpl implements DebtPositionService {

  public static final String HEADER_X_WORKFLOW_ID = "x-workflow-id";

  private final DebtPositionClient client;

  public DebtPositionServiceImpl(DebtPositionClient client) {
    this.client = client;
  }

  @Cacheable(cacheNames = CacheConfig.Fields.debtPositionTypeOrg, key = "#organizationId + '-' + #debtPositionTypeOrgCode", unless="#result == null")
  public DebtPositionTypeOrg getDebtPositionTypeOrgByOrgIdAndType(Long organizationId, String debtPositionTypeOrgCode, String accessToken) {
    return client.getDebtPositionTypeOrgByOrganizationIdAndCode(organizationId, debtPositionTypeOrgCode, accessToken);
  }

  @Cacheable(cacheNames = CacheConfig.Fields.debtPositionType, key = "#id", unless="#result == null")
  public DebtPositionType getDebtPositionTypeById(Long debtPositionType, String accessToken) {
    return client.getDebtPositionTypeById(debtPositionType, accessToken);
  }

  @Override
  public Long countExistingInstallmentsByIudIuvNav(Long organizationId, String iud, String iuv, String nav, String accessToken) {
    return client.countExistingInstallmentsByIudIuvNav(organizationId, iud, iuv, nav, accessToken);
  }

  @Override
  public Pair<DebtPositionDTO, String> createDebtPosition(DebtPositionDTO debtPositionDTO, String accessToken) {
    ResponseEntity<DebtPositionDTO> responseEntity = client.createDebtPosition(debtPositionDTO, accessToken);
    return Pair.of(responseEntity.getBody(), responseEntity.getHeaders().getFirst(HEADER_X_WORKFLOW_ID));
  }

  @Override
  public List<InstallmentDTO> getInstallmentsByOrganizationIdAndNav(Long organizationId, String nav, List<DebtPositionOrigin> debtPositionOrigin, String accessToken) {
    return client.getInstallmentsByOrganizationIdAndNav(organizationId, nav, debtPositionOrigin, accessToken);
  }

  @Override
  public DebtPositionDTO getDebtPositionByInstallmentId(Long installmentId, String accessToken) {
    return client.getDebtPositionByInstallmentId(installmentId, accessToken);
  }

  @Override
  public List<DebtPositionDTO> getDebtPositionsByOrganizationIdAndIuv(Long organizationId, String iuv, List<DebtPositionOrigin> debtPositionOrigin, String accessToken) {
    return client.getDebtPositionsByOrganizationIdAndIuv(organizationId, iuv, debtPositionOrigin, accessToken);
  }

  @Override
  public List<DebtPositionDTO> getDebtPositionsByOrganizationIdAndIud(Long organizationId, String iud, List<DebtPositionOrigin> debtPositionOrigin, String accessToken) {
    return client.getDebtPositionsByOrganizationIdAndIud(organizationId, iud, debtPositionOrigin, accessToken);
  }

  @Override
  public ReceiptDTO getReceiptById(Long receiptId, String accessToken) {
    return client.getReceiptById(receiptId, accessToken);
  }
}
