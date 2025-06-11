package it.gov.pagopa.pu.sil.connector.organization.service;

import it.gov.pagopa.pu.organization.dto.generated.Broker;
import it.gov.pagopa.pu.sil.connector.organization.client.BrokerClient;
import it.gov.pagopa.pu.sil.connector.organization.client.BrokerSearchClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class BrokerServiceTest {

  @Mock
  private BrokerSearchClient brokerSearchClientMock;

  @Mock
  private BrokerClient brokerClientMock;

  private BrokerService brokerService;

  private final String accessToken = "ACCESSTOKEN";

  @BeforeEach
  void init() {
    brokerService = new BrokerServiceImpl(brokerSearchClientMock, brokerClientMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(brokerSearchClientMock);
    Mockito.verifyNoMoreInteractions(brokerClientMock);
  }

  @Test
  void givenNotExistentOrgIdWhenGetOrganizationByIdThenEmpty() {
    // Given
    Long orgId = 1L;
    Mockito.when(brokerSearchClientMock.findByBrokeredOrganizationId(orgId, accessToken))
      .thenReturn(null);

    // When
    Optional<Broker> result = brokerService.getBrokerByBrokeredOrganizationId(orgId, accessToken);

    // Then
    Assertions.assertTrue(result.isEmpty());
  }

  @Test
  void givenExistentOrgIdWhenGetOrganizationByIdThenEmpty() {
    // Given
    Long orgId = 1L;
    Broker expectedResult = new Broker();
    Mockito.when(brokerSearchClientMock.findByBrokeredOrganizationId(orgId, accessToken))
      .thenReturn(expectedResult);

    // When
    Optional<Broker> result = brokerService.getBrokerByBrokeredOrganizationId(orgId, accessToken);

    // Then
    Assertions.assertTrue(result.isPresent());
    Assertions.assertSame(expectedResult, result.get());
  }

  @Test
  void givenValidBrokerIdWhenFindByIdThenOk() {
    // Given
    Long brokerId = 1L;
    Broker expectedResult = new Broker();
    Mockito.when(brokerClientMock.findById(brokerId, accessToken))
      .thenReturn(expectedResult);

    // When
    Broker result = brokerService.findById(brokerId, accessToken);

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertSame(expectedResult, result);
  }
}
