package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.mapper.PaaSILInviaDovutiMapper;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionCheckoutService;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovutiRisposta;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

@Service
@Slf4j
public class PaaSILInviaDovutiService extends AbstractImmediatePaymentsService<PaaSILInviaDovuti, PaaSILInviaDovutiRisposta> {

  private final PaaSILInviaDovutiMapper paaSILInviaDovutiMapper;

  public PaaSILInviaDovutiService(InstantPaymentsFacade instantPaymentsFacade,
                                  OrganizationService organizationService,
                                  DebtPositionCheckoutService debtPositionCheckoutService,
                                  PaaSILInviaDovutiMapper paaSILInviaDovutiMapper,
                                  SessionIdMapper sessionIdMapper) {
    super(instantPaymentsFacade, organizationService, sessionIdMapper, debtPositionCheckoutService);
    this.paaSILInviaDovutiMapper = paaSILInviaDovutiMapper;
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
