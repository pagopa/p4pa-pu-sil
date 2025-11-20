package it.gov.pagopa.pu.sil.connector.organization.service;

import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;

import java.util.List;
import java.util.Optional;

public interface OrganizationService {

  Optional<Organization> getOrganizationByFiscalCode(String orgFiscalCode, String accessToken);

  Optional<Organization> getOrganizationByIpaCode(String ipaCode, String accessToken);

  Optional<Organization> getOrganizationById(Long id, String accessToken);

  List<Organization> findByBrokerIdAndStatus(Long brokerId, OrganizationStatus status, String accessToken);
}
