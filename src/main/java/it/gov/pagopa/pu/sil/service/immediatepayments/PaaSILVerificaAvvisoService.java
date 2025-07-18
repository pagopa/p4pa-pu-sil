package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
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
import it.gov.pagopa.pu.sil.util.Utilities;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.veneto.regione.pagamenti.ente.PaaSILVerificaAvviso;
import it.veneto.regione.pagamenti.ente.PaaSILVerificaAvvisoRisposta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaaSILVerificaAvvisoService {

  private final CartRequestMapper cartRequestMapper;
  private final OrganizationService organizationService;
  private final CheckoutService checkoutService;
  private final InstallmentFacadeService installmentFacadeService;

  public PaaSILVerificaAvvisoRisposta processRequest(PaaSILVerificaAvviso request, String orgIpaCode, UserInfo userInfo, String accessToken) {
    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);
    //check if the logged user has the right to call this endpoint
    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call {} for organization {}", request.getClass().getSimpleName(), clientId, orgIpaCode);
      throw new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "Utente non autorizzato");
    }
    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);
    Organization organization = organizationService.getOrganizationById(organizationId, accessToken)
      .orElse(null);
    if (organization == null || !OrganizationStatus.ACTIVE.equals(organization.getStatus())) {
      throw new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "L'ente non è valido o non è abilitato");
    }

    //validate callback URL
    if (StringUtils.isNotBlank(request.getEnteSILInviaRispostaPagamentoUrl()) && !ValidationUtils.isValidUri(request.getEnteSILInviaRispostaPagamentoUrl())) {
      throw new SilFaultException(SilFaults.PAA_URL_NON_VALIDA, "URL di callback non valida");
    }

    // Validate the IUV
    if (StringUtils.isBlank(request.getIdentificativoUnivocoVersamento())) {
      throw new SilFaultException(SilFaults.PAA_IUV_NON_VALIDO, "IUV non indicato");
    }
    String nav = Utilities.iuv2Nav(request.getIdentificativoUnivocoVersamento());

    // search installments by IUV
    List<InstallmentDTO> installments = installmentFacadeService.getInstallmentsByOrganizationIdAndNav(organizationId, nav, accessToken);

    //filter installments to find if an unpaid installment exists
    // otherwise throw a SilFaultException
    InstallmentDTO payableInstallment = installments.stream()
      .filter(installment -> Objects.equals(installment.getStatus(), InstallmentStatus.UNPAID))
      .findFirst().orElseThrow(() -> new SilFaultException(SilFaults.PAA_IUV_NON_VALIDO,"Nessun avviso pagabile trovato per lo IUV indicato"));

    //sessionId is the installment ID, used to track the session
    String sessionId = String.valueOf(payableInstallment.getInstallmentId());

    String cartId = UUID.randomUUID().toString();

    //map debt positions to cart request
    CartRequest cartRequest = cartRequestMapper.mapInstallmentToCartRequest(payableInstallment, organization, cartId, request.getEnteSILInviaRispostaPagamentoUrl());

    //invoke carts API to trigger the payment on Checkout
    String checkoutUrl = checkoutService.checkoutCart(cartRequest);

    // Prepare the response
    PaaSILVerificaAvvisoRisposta response = new PaaSILVerificaAvvisoRisposta();
    response.setEsito(RegistryOutcome.OK.getValue());
    response.setUrl(checkoutUrl);
    response.setIdSession(sessionId);
    response.setRedirect(1); // 1 means redirect to the checkout URL
    return response;
  }
}
