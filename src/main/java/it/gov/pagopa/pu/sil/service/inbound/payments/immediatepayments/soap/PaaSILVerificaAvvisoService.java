package it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments.soap;

import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.inbound.payments.debtposition.DebtPositionCheckoutService;
import it.gov.pagopa.pu.sil.service.inbound.payments.debtposition.InstallmentFacadeService;
import it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments.BaseVerifyNoticeService;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.veneto.regione.pagamenti.ente.PaaSILVerificaAvviso;
import it.veneto.regione.pagamenti.ente.PaaSILVerificaAvvisoRisposta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaaSILVerificaAvvisoService extends BaseVerifyNoticeService<PaaSILVerificaAvviso, PaaSILVerificaAvvisoRisposta> {

  private final String auxDigit;

  public PaaSILVerificaAvvisoService(OrganizationService organizationService,
                                     InstallmentFacadeService installmentFacadeService,
                                     DebtPositionCheckoutService debtPositionCheckoutService,
                                     @Value("${nav.aux-digit}") String auxDigit) {
    super(organizationService, installmentFacadeService, debtPositionCheckoutService);
    this.auxDigit = auxDigit;
  }

  @Override
  protected String getNav(PaaSILVerificaAvviso request) {
    return Utilities.iuv2Nav(request.getIdentificativoUnivocoVersamento(), auxDigit);
  }

  @Override
  protected String getCallbackUrl(PaaSILVerificaAvviso request) {
    return request.getEnteSILInviaRispostaPagamentoUrl();
  }

  @Override
  protected PaaSILVerificaAvvisoRisposta mapToResponse(String outcome, String checkoutUrl, String sessionId) {
    PaaSILVerificaAvvisoRisposta response = new PaaSILVerificaAvvisoRisposta();
    response.setEsito(outcome);
    response.setUrl(checkoutUrl);
    response.setIdSession(sessionId);
    response.setRedirect(1); // 1 means redirect to the checkout URL
    return response;
  }

  @Override
  protected PaaSILVerificaAvvisoRisposta handleInstallmentsStatus(InstallmentDTO installment, Organization organization, String callbackUrl, String accessToken) {
    if(InstallmentStatus.UNPAID.equals(installment.getStatus())) {
      return doCheckOut(installment, organization, callbackUrl, accessToken);
    } else {
      throw new SilFaultException(SilFaults.PAA_IUV_NON_VALIDO,"Nessun avviso pagabile trovato per l'identificativo univoco del versamento indicato");
    }
  }
}
