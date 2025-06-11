package it.gov.pagopa.pu.sil.connector.organization.service;

import it.gov.pagopa.pu.organization.dto.generated.Broker;
import it.gov.pagopa.pu.sil.connector.organization.client.BrokerClient;
import it.gov.pagopa.pu.sil.connector.organization.client.BrokerSearchClient;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BrokerServiceImpl implements BrokerService {

  private final BrokerSearchClient brokerSearchClient;
  private final BrokerClient brokerClient;

  public BrokerServiceImpl(BrokerSearchClient brokerSearchClient, BrokerClient brokerClient) {
    this.brokerSearchClient = brokerSearchClient;
    this.brokerClient = brokerClient;
  }

  @Override
  public Optional<Broker> getBrokerByBrokeredOrganizationId(Long organizationId, String accessToken) {
    return Optional.ofNullable(
      brokerSearchClient.findByBrokeredOrganizationId(organizationId, accessToken)
    );
  }

  @Override
  public Broker findById(Long brokerId, String accessToken) {
    return brokerClient.findById(brokerId, accessToken);
  }
}
