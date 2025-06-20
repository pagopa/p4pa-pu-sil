package it.gov.pagopa.pu.sil.connector.organization.client;

import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
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

  public OrgSilService findById(String orgSilServiceId, String accessToken) {
    try {
      return organizationApisHolder.getOrgSilServiceEntityControllerApi(accessToken)
          .crudGetOrgsilservice(orgSilServiceId);
    } catch (Exception e) {
      log.info("Cannot find OrgSilService with id {}", orgSilServiceId, e);
      return null;
    }
  }
}
