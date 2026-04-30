package it.gov.pagopa.pu.sil.connector.pagopa.checkout;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.client.CheckoutClient;
import org.springframework.stereotype.Service;

@Service
public class CheckoutServiceImpl implements CheckoutService {

  private final CheckoutClient client;

  public CheckoutServiceImpl(CheckoutClient client) {
    this.client = client;
  }

  @Override
  public String checkoutCart(CartRequest cartRequest) {
    return client.checkoutCart(cartRequest);
  }
}
