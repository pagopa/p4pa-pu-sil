package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.CheckoutService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.debtposition.InstallmentFacadeService;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.UUID;

@Slf4j
public abstract class BaseVerifyNoticeService<I, O> {

  protected final CartRequestMapper cartRequestMapper;
  protected final OrganizationService organizationService;
  protected final CheckoutService checkoutService;
  protected final InstallmentFacadeService installmentFacadeService;

  protected BaseVerifyNoticeService(CartRequestMapper cartRequestMapper,
                                    OrganizationService organizationService,
                                    CheckoutService checkoutService,
                                    InstallmentFacadeService installmentFacadeService) {
    this.cartRequestMapper = cartRequestMapper;
    this.organizationService = organizationService;
    this.checkoutService = checkoutService;
    this.installmentFacadeService = installmentFacadeService;
  }

  public O processRequest(I request, String orgIpaCode, UserInfo userInfo, String accessToken) {
    AuthorizationService.validateAdminRole(orgIpaCode, userInfo);

    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);

    Organization organization = organizationService.getOrganizationById(organizationId, accessToken)
      .orElse(null);

    if (organization == null || !OrganizationStatus.ACTIVE.equals(organization.getStatus())) {
      throw new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "L'ente non è valido o non è abilitato");
    }

    String callbackUrl = getCallbackUrl(request);
    if (StringUtils.isNotBlank(callbackUrl) && !ValidationUtils.isValidUri(callbackUrl)) {
      throw new SilFaultException(SilFaults.PAA_URL_NON_VALIDA, "URL di callback non valida");
    }

    String iuv = getIuv(request);
    if (StringUtils.isBlank(iuv)) {
      throw new SilFaultException(SilFaults.PAA_IUV_NON_VALIDO, "Identificativo univoco del versamento non indicato");
    }

    List<InstallmentDTO> installments = installmentFacadeService.getInstallmentsByOrganizationIdAndNav(organizationId, iuv, accessToken);

    InstallmentDTO installmentDTO = installments.stream()
      .findFirst().orElseThrow(() -> new SilFaultException(SilFaults.PAA_IUV_NON_VALIDO,"Nessun avviso pagabile trovato per l'dentificativo univoco del versamento indicato"));

    return handleInstallmentsStatus(installmentDTO, organization, callbackUrl);
  }

  protected abstract String getIuv(I request);
  protected abstract String getCallbackUrl(I request);
  protected abstract O mapToResponse(String outcome, String checkoutUrl, String sessionId);
  protected abstract O handleInstallmentsStatus(InstallmentDTO installment, Organization organization, String callbackUrl);

  protected O doCheckOut(InstallmentDTO payableInstallment, Organization organization, String callbackUrl) {
    String sessionId = String.valueOf(payableInstallment.getInstallmentId());
    String cartId = UUID.randomUUID().toString();
    CartRequest cartRequest = cartRequestMapper.mapInstallmentToCartRequest(payableInstallment, organization, cartId, callbackUrl);
    String checkoutUrl = checkoutService.checkoutCart(cartRequest);
    if(StringUtils.isBlank(checkoutUrl)){
      throw new SilFaultException(SilFaults.PAA_SYSTEM_ERROR, "Errore durante la creazione del carrello di pagamento");
    }
    return mapToResponse(RegistryOutcome.OK.getValue(), checkoutUrl, sessionId);
  }
}
