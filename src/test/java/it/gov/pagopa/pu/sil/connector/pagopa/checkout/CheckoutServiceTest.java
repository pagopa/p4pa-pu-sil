package it.gov.pagopa.pu.sil.connector.pagopa.checkout;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.client.CheckoutClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

  @Mock
  private CheckoutClient clientMock;

  private CheckoutService service;

  @BeforeEach
  void init(){
    service = new CheckoutServiceImpl(clientMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(clientMock);
  }

  @Test
  void whenThenInvokeClient(){
    // Given
    CartRequest request = new CartRequest();
    String expectedResult = "LOCATION";
    Mockito.when(clientMock.checkoutCart(Mockito.same(request))).thenReturn(expectedResult);

    //When
    String result = service.checkoutCart(request);

    // Then
    Assertions.assertSame(expectedResult, result);
  }
}
