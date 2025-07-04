package it.gov.pagopa.pu.sil.connector.organization.service;

import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;

import java.util.Optional;

public interface OrgSilServiceComponent {
  Optional<OrgSilServiceDTO> getOrgSilServiceById(Long orgSilServiceId, String accessToken);
}
