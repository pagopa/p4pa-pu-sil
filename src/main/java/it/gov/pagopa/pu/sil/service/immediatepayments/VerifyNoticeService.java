package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.PaymentResponse;
import it.gov.pagopa.pu.sil.dto.generated.PaymentResponse.OutcomeEnum;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionCheckoutService;
import it.gov.pagopa.pu.sil.service.debtposition.InstallmentFacadeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class VerifyNoticeService extends BaseVerifyNoticeService<Pair<String, String>, PaymentResponse> {
  private final String puSilBaseUrl;

  public VerifyNoticeService(OrganizationService organizationService,
                             InstallmentFacadeService installmentFacadeService,
                             DebtPositionCheckoutService debtPositionCheckoutService,
                             @Value("${public-base-url.pu-sil}") String puSilBaseUrl) {
    super(organizationService, installmentFacadeService, debtPositionCheckoutService);
    this.puSilBaseUrl = puSilBaseUrl;
  }

  @Override
  protected String getNav(Pair<String, String> request) {
    return request.getLeft(); // nav
  }

  @Override
  protected String getCallbackUrl(Pair<String, String> request) {
    return request.getRight(); // callbackUrl
  }

  @Override
  protected PaymentResponse mapToResponse(String outcome, String checkoutUrl, String sessionId) {
    return PaymentResponse.builder()
      .outcome(OutcomeEnum.OK)
      .triggerPaymentUrl(checkoutUrl)
      .paymentId(sessionId)
      .build();
  }

  @Override
  protected PaymentResponse handleInstallmentsStatus(InstallmentDTO installment, Organization organization, String callbackUrl, String accessToken) {
    PaymentResponse response = new PaymentResponse();
    return switch (installment.getStatus()) {
      case UNPAID -> doCheckOut(installment, organization, callbackUrl, accessToken)
        .downloadNoticeUrl(composeDownloadNoticeUrl(organization.getOrgFiscalCode(), installment.getNav()));
      case PAID -> response.outcome(OutcomeEnum.ALREADY_PAID);
      case UNPAYABLE -> response.outcome(OutcomeEnum.NOT_PAYABLE);
      default -> response.outcome(OutcomeEnum.NOT_FOUND);
    };
  }

  private String composeDownloadNoticeUrl(String orgFiscalCode, String nav) {
    return UriComponentsBuilder.fromUriString(puSilBaseUrl)
      .path("/organization/{orgFiscalCode}/printpaymentnotice/{nav}")
      .buildAndExpand(orgFiscalCode, nav)
      .toUriString();
  }
}
