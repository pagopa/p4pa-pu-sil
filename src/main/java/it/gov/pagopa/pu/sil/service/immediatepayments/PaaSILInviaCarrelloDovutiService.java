package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.mapper.PaaSILInviaCarrelloDovutiMapper;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionCheckoutService;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovutiRisposta;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

@Service
@Slf4j
public class PaaSILInviaCarrelloDovutiService extends AbstractImmediatePaymentsService<PaaSILInviaCarrelloDovuti, PaaSILInviaCarrelloDovutiRisposta> {

  private final PaaSILInviaCarrelloDovutiMapper paaSILInviaCarrelloDovutiMapper;

  public PaaSILInviaCarrelloDovutiService(InstantPaymentsFacade instantPaymentsFacade,
                                          OrganizationService organizationService,
                                          DebtPositionCheckoutService debtPositionCheckoutService,
                                          PaaSILInviaCarrelloDovutiMapper paaSILInviaCarrelloDovutiMapper,
                                          SessionIdMapper sessionIdMapper) {
    super(instantPaymentsFacade, organizationService, sessionIdMapper, debtPositionCheckoutService);
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
