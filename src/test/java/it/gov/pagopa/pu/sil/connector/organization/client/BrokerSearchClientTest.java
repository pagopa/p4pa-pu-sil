package it.gov.pagopa.pu.sil.connector.organization.client;

import it.gov.pagopa.pu.organization.client.generated.BrokerSearchControllerApi;
import it.gov.pagopa.pu.organization.dto.generated.Broker;
import it.gov.pagopa.pu.sil.connector.organization.config.OrganizationApisHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
class BrokerSearchClientTest {
  @Mock
  private OrganizationApisHolder organizationApisHolderMock;
  @Mock
  private BrokerSearchControllerApi brokerSearchControllerApiMock;

  private BrokerSearchClient brokerSearchClient;

  @BeforeEach
  void setUp() {
    brokerSearchClient = new BrokerSearchClient(organizationApisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      organizationApisHolderMock,
      brokerSearchControllerApiMock
    );
  }

  @Test
  void whenFindByBrokeredOrganizationIdThenInvokeWithAccessToken() {
    // Given
    String accessToken = "ACCESSTOKEN";
    String orgId = "1";
    Broker expectedResult = new Broker();

    Mockito.when(organizationApisHolderMock.getBrokerSearchControllerApi(accessToken))
      .thenReturn(brokerSearchControllerApiMock);
    Mockito.when(brokerSearchControllerApiMock.crudBrokersFindByBrokeredOrganizationId(orgId))
      .thenReturn(expectedResult);

    // When
    Broker result = brokerSearchClient.findByBrokeredOrganizationId(Long.valueOf(orgId), accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }

  @Test
  void givenNotExistedOrganizationIdWhenFindByBrokeredOrganizationIdThenNull() {
    // Given
    String accessToken = "ACCESSTOKEN";
    String orgId = "1";

    Mockito.when(organizationApisHolderMock.getBrokerSearchControllerApi(accessToken))
      .thenReturn(brokerSearchControllerApiMock);
    Mockito.when(brokerSearchControllerApiMock.crudBrokersFindByBrokeredOrganizationId(orgId))
      .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "NotFound", null, null, null));

    // When
    Broker result = brokerSearchClient.findByBrokeredOrganizationId(Long.valueOf(orgId), accessToken);

    // Then
    Assertions.assertNull(result);
  }
}
