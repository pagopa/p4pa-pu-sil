package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.CheckoutService;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.mapper.PaaSILInviaDovutiMapper;
import it.gov.pagopa.pu.sil.service.debtposition.CreateDebtPositionService;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovutiRisposta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PaaSILInviaDovutiService extends AbstractImmediatePaymentsService<PaaSILInviaDovuti, PaaSILInviaDovutiRisposta> {

  private final PaaSILInviaDovutiMapper paaSILInviaDovutiMapper;

  public PaaSILInviaDovutiService(CheckoutService checkoutService,
                                  CreateDebtPositionService createDebtPositionService,
                                  CartRequestMapper cartRequestMapper,
                                  PaaSILInviaDovutiMapper paaSILInviaDovutiMapper) {
    super(checkoutService, createDebtPositionService, cartRequestMapper);
    this.paaSILInviaDovutiMapper = paaSILInviaDovutiMapper;
  }

  @Override
  protected List<DebtPositionDTO> mapRequestToDebtPositions(PaaSILInviaDovuti request, String cartId, UserInfo userInfo, String orgIpaCode, String accessToken) {
    return paaSILInviaDovutiMapper.mapRequestToDebtPositions(request, cartId, userInfo, orgIpaCode, accessToken);
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
