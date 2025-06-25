package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.nodo.checkout.dto.generated.CartRequestReturnUrls;
import it.gov.pagopa.nodo.checkout.dto.generated.PaymentNotice;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Service
@Slf4j
public class CartRequestMapper {

  private final String defaultCallbackUrlOk;
  private final String defaultCallbackUrlKo;
  private final String defaultCallbackUrlCancel;

  public CartRequestMapper(@Value("${checkout.default-callback-url.ok}") String defaultCallbackUrlOk,
                           @Value("${checkout.default-callback-url.ko}") String defaultCallbackUrlKo,
                           @Value("${checkout.default-callback-url.cancel}") String defaultCallbackUrlCancel) {
    this.defaultCallbackUrlOk = defaultCallbackUrlOk;
    this.defaultCallbackUrlKo = defaultCallbackUrlKo;
    this.defaultCallbackUrlCancel = defaultCallbackUrlCancel;
  }


  public CartRequest mapDebtPositionsToCartRequest(List<DebtPositionDTO> debtPositions,
                                                   String cartId, String requestCallbackUrl) {
    List<PaymentNotice> paymentNotices = debtPositions.stream()
      .flatMap(dp -> dp.getPaymentOptions().stream())
      .flatMap(option -> option.getInstallments().stream())
      .map(installment -> (PaymentNotice) PaymentNotice.builder()
        .noticeNumber(installment.getNav())
        .fiscalCode(installment.getTransfers().getFirst().getOrgFiscalCode())
        .amount(installment.getAmountCents().intValue())
        .companyName(installment.getTransfers().getFirst().getOrgName())
        .description(installment.getRemittanceInformation())
        .build())
      .toList();

    boolean allCCP = debtPositions.stream()
      .flatMap(dp -> dp.getPaymentOptions().stream())
      .flatMap(option -> option.getInstallments().stream())
      .flatMap(installment -> installment.getTransfers().stream())
      .allMatch(transfer -> StringUtils.isNotBlank(transfer.getPostalIban()));

    URI callbackUriOk, callbackUriKo, callbackUriCancel;
    try {
      callbackUriOk = new URI(StringUtils.firstNonBlank(requestCallbackUrl, defaultCallbackUrlOk));
      callbackUriKo = new URI(StringUtils.firstNonBlank(requestCallbackUrl, defaultCallbackUrlKo));
      callbackUriCancel = new URI(StringUtils.firstNonBlank(requestCallbackUrl, defaultCallbackUrlCancel));
    } catch (URISyntaxException use) {
      throw new ApplicationException(use);
    }

    return CartRequest.builder()
      .idCart(cartId)
      .emailNotice(debtPositions.getFirst().getPaymentOptions().getFirst().getInstallments().getFirst().getDebtor().getEmail())
      .paymentNotices(paymentNotices)
      .returnUrls(CartRequestReturnUrls.builder()
        .returnOkUrl(callbackUriOk)
        .returnErrorUrl(callbackUriKo)
        .returnCancelUrl(callbackUriCancel)
        .build())
      .allCCP(allCCP)
      .build();
  }
}
