package it.gov.pagopa.pu.sil.connector.organization.client;

import it.gov.pagopa.pu.organization.dto.generated.Broker;
import it.gov.pagopa.pu.sil.connector.organization.config.OrganizationApisHolder;
import org.springframework.stereotype.Service;

@Service
public class BrokerClient {

  private final OrganizationApisHolder organizationApisHolder;

  public BrokerClient(OrganizationApisHolder organizationApisHolder) {
    this.organizationApisHolder = organizationApisHolder;
  }

  public Broker findById(Long brokerId, String accessToken) {
    return organizationApisHolder.getBrokerEntityControllerApi(accessToken)
      .crudGetBroker(String.valueOf(brokerId));
  }

}
