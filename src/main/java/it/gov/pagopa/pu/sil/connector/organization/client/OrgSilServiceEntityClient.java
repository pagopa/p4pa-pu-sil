package it.gov.pagopa.pu.sil.connector.organization.client;

import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.organization.config.OrganizationApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrgSilServiceEntityClient {
  private final OrganizationApisHolder organizationApisHolder;

  public OrgSilServiceEntityClient(OrganizationApisHolder organizationApisHolder) {
    this.organizationApisHolder = organizationApisHolder;
  }

  public OrgSilServiceDTO findById(Long orgSilServiceId, String accessToken) {
    try {
      return organizationApisHolder.getOrganizationSilServiceApi(accessToken)
          .getOrgSilService(orgSilServiceId);
    } catch (Exception e) {
      log.info("Cannot find OrgSilService with id {}", orgSilServiceId, e);
      return null;
    }
  }
}
