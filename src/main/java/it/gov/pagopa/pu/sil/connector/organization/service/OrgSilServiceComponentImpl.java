package it.gov.pagopa.pu.sil.connector.organization.service;

import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.organization.client.OrgSilServiceEntityClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrgSilServiceComponentImpl implements OrgSilServiceComponent {
  private final OrgSilServiceEntityClient orgSilServiceEntityClient;

  public OrgSilServiceComponentImpl(OrgSilServiceEntityClient orgSilServiceEntityClient) {
    this.orgSilServiceEntityClient = orgSilServiceEntityClient;
  }

  @Override
  public Optional<OrgSilService> getOrgSilServiceById(Long orgSilServiceId, String accessToken) {
    return Optional.ofNullable(
      orgSilServiceEntityClient.findById(String.valueOf(orgSilServiceId), accessToken)
    );
  }
}
