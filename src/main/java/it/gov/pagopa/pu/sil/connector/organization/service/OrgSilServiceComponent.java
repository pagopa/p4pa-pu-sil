package it.gov.pagopa.pu.sil.connector.organization.service;

import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;

import java.util.Optional;

public interface OrgSilServiceComponent {
  Optional<OrgSilService> getOrgSilServiceById(String orgSilServiceId, String accessToken);
}
