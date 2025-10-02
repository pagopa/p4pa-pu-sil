package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.CheckoutService;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.mapper.PaaSILInviaDovutiMapper;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovutiRisposta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaaSILInviaDovutiService extends AbstractImmediatePaymentsService<PaaSILInviaDovuti, PaaSILInviaDovutiRisposta> {

  private final PaaSILInviaDovutiMapper paaSILInviaDovutiMapper;

  public PaaSILInviaDovutiService(CheckoutService checkoutService,
                                  InstantPaymentsFacade instantPaymentsFacade,
                                  CartRequestMapper cartRequestMapper,
                                  OrganizationService organizationService,
                                  PaaSILInviaDovutiMapper paaSILInviaDovutiMapper,
                                  SessionIdMapper sessionIdMapper) {
    super(checkoutService, instantPaymentsFacade, organizationService, cartRequestMapper, sessionIdMapper);
    this.paaSILInviaDovutiMapper = paaSILInviaDovutiMapper;
  }

  @Override
  protected PaymentRequestMappingResult mapRequestToDebtPositions(PaaSILInviaDovuti request, Organization org, String cartId, String accessToken) {
    return paaSILInviaDovutiMapper.mapRequestToDebtPositions(request, org, cartId, accessToken);
  }

  @Override
  protected PaaSILInviaDovutiRisposta mapToResponse(String outcome, String checkoutUrl, String sessionId) {
    PaaSILInviaDovutiRisposta response = new PaaSILInviaDovutiRisposta();
    response.setEsito(RegistryOutcome.OK.getValue());
    response.setUrl(checkoutUrl);
    response.setIdSession(sessionId);
    response.setRedirect(1); // 1 means redirect to the checkout URL
    return response;
  }

  @Override
  protected String getCallbackUrl(PaaSILInviaDovuti request) {
    return request.getEnteSILInviaRispostaPagamentoUrl();
  }
}
