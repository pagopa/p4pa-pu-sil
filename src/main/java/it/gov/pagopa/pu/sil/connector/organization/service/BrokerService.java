package it.gov.pagopa.pu.sil.connector.organization.service;

import it.gov.pagopa.pu.organization.dto.generated.Broker;

import java.util.Optional;

public interface BrokerService {

  Optional<Broker> getBrokerByBrokeredOrganizationId(Long organizationId, String accessToken);

  Broker findById(Long brokerId, String accessToken);
}
