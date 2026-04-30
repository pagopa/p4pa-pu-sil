package it.gov.pagopa.pu.sil.connector.organization.service;

import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
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
  public Optional<OrgSilServiceDTO> getOrgSilServiceById(Long orgSilServiceId, String accessToken) {
    return Optional.ofNullable(
      orgSilServiceEntityClient.findById(orgSilServiceId, accessToken)
    );
  }
}
