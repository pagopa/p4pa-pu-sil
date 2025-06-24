package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.CheckoutService;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.mapper.PaaSILInviaCarrelloDovutiMapper;
import it.gov.pagopa.pu.sil.service.debtposition.CreateDebtPositionService;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovutiRisposta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PaaSILInviaCarrelloDovutiService extends AbstractImmediatePaymentsService<PaaSILInviaCarrelloDovuti, PaaSILInviaCarrelloDovutiRisposta> {

  private final PaaSILInviaCarrelloDovutiMapper paaSILInviaCarrelloDovutiMapper;

  public PaaSILInviaCarrelloDovutiService(CheckoutService checkoutService,
                                          CreateDebtPositionService createDebtPositionService,
                                          CartRequestMapper cartRequestMapper,
                                          PaaSILInviaCarrelloDovutiMapper paaSILInviaCarrelloDovutiMapper) {
    super(checkoutService, createDebtPositionService, cartRequestMapper);
    this.paaSILInviaCarrelloDovutiMapper = paaSILInviaCarrelloDovutiMapper;
  }

  @Override
  protected List<DebtPositionDTO> mapRequestToDebtPositions(PaaSILInviaCarrelloDovuti request, String cartId, UserInfo userInfo, String orgIpaCode, String accessToken) {
    return paaSILInviaCarrelloDovutiMapper.mapRequestToDebtPositions(request, cartId, userInfo, orgIpaCode, accessToken);
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
