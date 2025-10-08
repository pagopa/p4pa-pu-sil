package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.CheckoutService;
import it.gov.pagopa.pu.sil.dto.generated.InstantPaymentRequest;
import it.gov.pagopa.pu.sil.dto.generated.PaymentResponse;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.mapper.InstantPaymentMapper;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class InstantPaymentService extends AbstractImmediatePaymentsService<InstantPaymentRequest, PaymentResponse> {
  private final InstantPaymentMapper instantPaymentMapper;

  protected InstantPaymentService(CheckoutService checkoutService,
                                  InstantPaymentsFacade instantPaymentsFacade,
                                  OrganizationService organizationService,
                                  CartRequestMapper cartRequestMapper,
                                  SessionIdMapper sessionIdMapper,
                                  InstantPaymentMapper instantPaymentMapper) {
    super(checkoutService, instantPaymentsFacade, organizationService, cartRequestMapper, sessionIdMapper);
    this.instantPaymentMapper = instantPaymentMapper;
  }

  @Override
  protected List<DebtPositionDTO> createDebtPositionsFromMapping(PaymentRequestMappingResult paymentRequestMappingResult, String accessToken) {
    return instantPaymentsFacade.createDebtPositionsFromMapping(paymentRequestMappingResult, accessToken);
  }

  @Override
  protected PaymentRequestMappingResult mapRequestToDebtPositions(InstantPaymentRequest request, Organization org, String cartId, String accessToken) {
    List<DebtPositionDTO> dp = instantPaymentMapper.mapRequestToDebtPositions(request, org, cartId, accessToken);
    return PaymentRequestMappingResult.ofDebtPositions(dp);
  }

  @Override
  protected PaymentResponse mapToResponse(String outcome, String checkoutUrl, String sessionId) {
    return PaymentResponse.builder()
      .outcome(PaymentResponse.OutcomeEnum.fromValue(outcome))
      .triggerPaymentUrl(checkoutUrl)
      .paymentId(sessionId)
      .build();
  }

  @Override
  protected String getCallbackUrl(InstantPaymentRequest request) {
    return request.getCallbackUrl();
  }
}
