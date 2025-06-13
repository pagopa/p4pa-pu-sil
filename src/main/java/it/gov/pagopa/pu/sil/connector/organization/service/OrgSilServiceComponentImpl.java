package it.gov.pagopa.pu.sil.connector.organization.service;

import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.organization.client.OrgSilServiceEntityClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrgSilServiceComponentImpl implements OrgSilServiceComponent {
  private final OrgSilServiceEntityClient orgSilServiceEntityClient;

  public OrgSilServiceComponentImpl(OrgSilServiceEntityClient orgSilServiceEntityClient) {
    this.orgSilServiceEntityClient = orgSilServiceEntityClient;
  }

  @Override
  public Optional<OrgSilService> getOrgSilServiceById(String orgSilServiceId, String accessToken) {
    return Optional.ofNullable(
      orgSilServiceEntityClient.findById(orgSilServiceId, accessToken)
    );
  }
}
