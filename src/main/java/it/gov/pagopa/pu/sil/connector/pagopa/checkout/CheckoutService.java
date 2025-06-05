package it.gov.pagopa.pu.sil.connector.pagopa.checkout;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;

public interface CheckoutService {
  String checkoutCart(CartRequest cartRequest);
}
