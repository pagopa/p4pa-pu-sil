package it.gov.pagopa.pu.sil.connector.organization.client;

import it.gov.pagopa.pu.organization.client.generated.BrokerEntityControllerApi;
import it.gov.pagopa.pu.organization.dto.generated.Broker;
import it.gov.pagopa.pu.sil.connector.organization.config.OrganizationApisHolder;
import it.gov.pagopa.pu.sil.exception.common.RestInvokeNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrokerClientTest {
  @Mock
  private OrganizationApisHolder organizationApisHolderMock;
  @Mock
  private BrokerEntityControllerApi brokerEntityControllerApiMock;

  private BrokerClient brokerClient;

  @BeforeEach
  void setUp() {
    brokerClient = new BrokerClient(organizationApisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      organizationApisHolderMock
    );
  }

  @Test
  void givenValidBrokerIdWhenFindByIdThenBroker() {
    // Given
    String accessToken = "ACCESS_TOKEN";
    long brokerId = 1L;
    Broker expectedResult = new Broker();

    when(organizationApisHolderMock.getBrokerEntityControllerApi(accessToken))
      .thenReturn(brokerEntityControllerApiMock);
    when(brokerEntityControllerApiMock.crudGetBroker(Long.toString(brokerId)))
      .thenReturn(expectedResult);

    // When
    Broker result = brokerClient.findById(brokerId, accessToken);

    // Then
    Assertions.assertSame(expectedResult, result);
  }

  @Test
  void givenNotExistentBrokerWhenFindByIdThenNull(){
    //Given
    String accessToken = "ACCESS_TOKEN";
    long brokerId = 1L;

    when(organizationApisHolderMock.getBrokerEntityControllerApi(accessToken))
      .thenReturn(brokerEntityControllerApiMock);
    when(brokerEntityControllerApiMock.crudGetBroker(Long.toString(brokerId)))
      .thenThrow(new RestInvokeNotFoundException("APPNAME", HttpStatus.NOT_FOUND, "ERROR", "ERRORCODE", "ERRORMESSAGE"));

    // When
    Broker result = brokerClient.findById(brokerId, accessToken);

    // Then
    Assertions.assertNull(result);
  }
}
