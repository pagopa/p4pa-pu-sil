package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.CheckoutService;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.mapper.PaaSILInviaCarrelloDovutiMapper;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovutiRisposta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import java.util.List;

@Service
@Slf4j
public class PaaSILInviaCarrelloDovutiService extends AbstractImmediatePaymentsService<PaaSILInviaCarrelloDovuti, PaaSILInviaCarrelloDovutiRisposta> {

  private final PaaSILInviaCarrelloDovutiMapper paaSILInviaCarrelloDovutiMapper;

  public PaaSILInviaCarrelloDovutiService(CheckoutService checkoutService,
                                          InstantPaymentsFacade instantPaymentsFacade,
                                          CartRequestMapper cartRequestMapper,
                                          OrganizationService organizationService,
                                          PaaSILInviaCarrelloDovutiMapper paaSILInviaCarrelloDovutiMapper,
                                          SessionIdMapper sessionIdMapper) {
    super(checkoutService, instantPaymentsFacade, organizationService, cartRequestMapper, sessionIdMapper);
    this.paaSILInviaCarrelloDovutiMapper = paaSILInviaCarrelloDovutiMapper;
  }

  @Override
  protected List<DebtPositionDTO> createDebtPositionsFromMapping(PaymentRequestMappingResult paymentRequestMappingResult, String accessToken) {
    try {
      return instantPaymentsFacade.createDebtPositionsFromMapping(paymentRequestMappingResult, accessToken);
    } catch (HttpServerErrorException e) {
      throw buildException(e);
    }
  }

  @Override
  protected PaymentRequestMappingResult mapRequestToDebtPositions(PaaSILInviaCarrelloDovuti request, Organization org, String cartId, String accessToken) {
    return paaSILInviaCarrelloDovutiMapper.mapRequestToDebtPositions(request, org, cartId, accessToken);
  }

  @Override
  protected PaaSILInviaCarrelloDovutiRisposta mapToResponse(String outcome, String checkoutUrl, String sessionId) {
    PaaSILInviaCarrelloDovutiRisposta response = new PaaSILInviaCarrelloDovutiRisposta();
    response.setEsito(RegistryOutcome.OK.getValue());
    response.setUrl(checkoutUrl);
    response.setIdSessionCarrello(sessionId);
    response.setRedirect(1); // 1 means redirect to the checkout URL
    return response;
  }

  @Override
  protected String getCallbackUrl(PaaSILInviaCarrelloDovuti request) {
    return request.getEnteSILInviaRispostaPagamentoUrl();
  }
}
