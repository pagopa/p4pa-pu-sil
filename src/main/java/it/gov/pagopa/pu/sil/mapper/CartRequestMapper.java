package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.nodo.checkout.dto.generated.CartRequestReturnUrls;
import it.gov.pagopa.nodo.checkout.dto.generated.PaymentNotice;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.exception.common.InvalidValueException;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

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


  public CartRequest mapDebtPositionsToCartRequest(List<DebtPositionDTO> debtPositions, Organization org,
                                                   String cartId, String requestCallbackUrl) {
    List<PaymentNotice> paymentNotices = debtPositions.stream()
      .flatMap(dp -> dp.getPaymentOptions().stream())
      .flatMap(option -> option.getInstallments().stream())
      .map(installment -> (PaymentNotice) PaymentNotice.builder()
        .noticeNumber(Objects.requireNonNull(installment.getNav()))
        .fiscalCode(org.getOrgFiscalCode())
        .amount(installment.getAmountCents().intValue())
        .companyName(org.getOrgName())
        .description(installment.getRemittanceInformation())
        .build())
      .toList();

    boolean allCCP = debtPositions.stream()
      .flatMap(dp -> dp.getPaymentOptions().stream())
      .flatMap(option -> option.getInstallments().stream())
      .flatMap(installment -> Objects.requireNonNull(installment.getTransfers()).stream())
      .allMatch(transfer -> StringUtils.isNotBlank(transfer.getPostalIban()));

    URI callbackUriOk;
    URI callbackUriKo;
    URI callbackUriCancel;
    try {
      callbackUriOk = new URI(StringUtils.firstNonBlank(requestCallbackUrl, defaultCallbackUrlOk));
      callbackUriKo = new URI(StringUtils.firstNonBlank(requestCallbackUrl, defaultCallbackUrlKo));
      callbackUriCancel = new URI(StringUtils.firstNonBlank(requestCallbackUrl, defaultCallbackUrlCancel));
    } catch (URISyntaxException e) {
      throw new InvalidValueException(ErrorCodeConstants.ERROR_CODE_INVALID_CALLBACK_URL, "Malformed DebtPosition callback URI", e);
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

  public CartRequest mapInstallmentToCartRequest(InstallmentDTO installment, Organization org,
                                                 String cartId, String requestCallbackUrl) {
    PaymentNotice paymentNotice = PaymentNotice.builder()
      .noticeNumber(Objects.requireNonNull(installment.getNav()))
      .fiscalCode(org.getOrgFiscalCode())
      .amount(installment.getAmountCents().intValue())
      .companyName(org.getOrgName())
      .description(installment.getRemittanceInformation())
      .build();

    boolean allCCP = Objects.requireNonNull(installment.getTransfers()).stream()
      .allMatch(transfer -> StringUtils.isNotBlank(transfer.getPostalIban()));

    URI callbackUriOk;
    URI callbackUriKo;
    URI callbackUriCancel;
    try {
      callbackUriOk = new URI(StringUtils.firstNonBlank(requestCallbackUrl, defaultCallbackUrlOk));
      callbackUriKo = new URI(StringUtils.firstNonBlank(requestCallbackUrl, defaultCallbackUrlKo));
      callbackUriCancel = new URI(StringUtils.firstNonBlank(requestCallbackUrl, defaultCallbackUrlCancel));
    } catch (URISyntaxException e) {
      throw new InvalidValueException(ErrorCodeConstants.ERROR_CODE_INVALID_CALLBACK_URL, "Malformed Installment callback URI", e);
    }

    return CartRequest.builder()
      .idCart(cartId)
      .emailNotice(installment.getDebtor().getEmail())
      .paymentNotices(List.of(paymentNotice))
      .returnUrls(CartRequestReturnUrls.builder()
        .returnOkUrl(callbackUriOk)
        .returnErrorUrl(callbackUriKo)
        .returnCancelUrl(callbackUriCancel)
        .build())
      .allCCP(allCCP)
      .build();
  }
}
