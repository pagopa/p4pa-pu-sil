package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.nodo.checkout.dto.generated.CartRequestReturnUrls;
import it.gov.pagopa.nodo.checkout.dto.generated.PaymentNotice;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartRequestMapper {

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

    URI callbackUri;
    try {
      //TODO handle case where callback URL is not provided P4ADEV-3220
      callbackUri = new URI(StringUtils.firstNonBlank(requestCallbackUrl, "http://TODO.com"));
    } catch (URISyntaxException use) {
      throw new ApplicationException(use);
    }

    return CartRequest.builder()
      .idCart(cartId)
      .emailNotice(debtPositions.getFirst().getPaymentOptions().getFirst().getInstallments().getFirst().getDebtor().getEmail())
      .paymentNotices(paymentNotices)
      .returnUrls(CartRequestReturnUrls.builder()
        .returnOkUrl(callbackUri)
        .returnErrorUrl(callbackUri)
        .returnCancelUrl(callbackUri)
        .build())
      .allCCP(allCCP)
      .build();
  }
}
