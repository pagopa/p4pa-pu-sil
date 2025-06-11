package it.gov.pagopa.pu.sil.connector.organization.client;

import it.gov.pagopa.pu.organization.dto.generated.Broker;
import it.gov.pagopa.pu.sil.connector.organization.config.OrganizationApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
@Slf4j
public class BrokerClient {

  private final OrganizationApisHolder organizationApisHolder;

  public BrokerClient(OrganizationApisHolder organizationApisHolder) {
    this.organizationApisHolder = organizationApisHolder;
  }

  public Broker findById(Long brokerId, String accessToken) {
    try {
      return organizationApisHolder.getBrokerEntityControllerApi(accessToken)
        .crudGetBroker(String.valueOf(brokerId));
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Cannot find Broker having id[{}]", brokerId, e);
      return null;
    }
  }

}
