package it.gov.pagopa.pu.sil.service.debtposition;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.LimitedTokenRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserInfoLimitedScope;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionOrigin;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.CheckoutService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static it.gov.pagopa.pu.sil.util.Utilities.IUV_SEPARATOR;

@Service
public class DebtPositionCheckoutService {

  public static final String PU_SIL_SCOPE = "pu-sil";
  public static final String CHECKOUT_RESOURCE = "CHECKOUT";
  public static final String CALLBACK_URL_SESSION_DATA_KEY = "callbackUrl";

  private final String applicationBaseUrl;

  private final OrganizationService organizationService;
  private final DebtPositionService debtPositionService;
  private final CheckoutService checkoutService;
  private final AuthorizationService authorizationService;
  private final CartRequestMapper cartRequestMapper;

  public DebtPositionCheckoutService(
      @Value("${public-base-url.pu-sil}") String applicationBaseUrl,
      OrganizationService organizationService,
      DebtPositionService debtPositionService, CheckoutService checkoutService,
      AuthorizationService authorizationService,
      CartRequestMapper cartRequestMapper) {
    this.applicationBaseUrl = applicationBaseUrl;
    this.organizationService = organizationService;
    this.debtPositionService = debtPositionService;
    this.checkoutService = checkoutService;
    this.authorizationService = authorizationService;
    this.cartRequestMapper = cartRequestMapper;
  }

  public String redirectToCheckout(UserInfo loggedUser, String accessToken) {
    UserInfoLimitedScope loggedUserLimitedScope = checkUserInfoLimitedScope(
        loggedUser);

    Optional<Organization> optionalOrganization = organizationService.getOrganizationById(
        loggedUserLimitedScope.getResource().getOrganization()
            .getOrganizationId(),
        accessToken);

    if (optionalOrganization.isEmpty()) {
      throw new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO);
    }

    Organization organization = optionalOrganization.get();

    List<String> iuvs = Arrays.stream(
        loggedUserLimitedScope.getResource().getResourceId()
            .split(IUV_SEPARATOR))
        .toList();

    List<DebtPositionDTO> debtPositions = iuvs.stream().map(
        iuv -> debtPositionService.getDebtPositionsByOrganizationIdAndIuv(
            organization.getOrganizationId(), iuv,
            List.of(DebtPositionOrigin.SPONTANEOUS_SIL), accessToken))
        .flatMap(List::stream).toList();

    String cartId = UUID.randomUUID().toString();
    String requestCallbackUrl = !CollectionUtils.isEmpty(loggedUserLimitedScope.getResource().getSessionData())
      && loggedUserLimitedScope.getResource().getSessionData().get(CALLBACK_URL_SESSION_DATA_KEY) != null
        ? loggedUserLimitedScope.getResource().getSessionData().get(
            CALLBACK_URL_SESSION_DATA_KEY).toString()
        : null;
    CartRequest cartRequest = cartRequestMapper.mapDebtPositionsToCartRequest(
        debtPositions, organization, cartId,
        requestCallbackUrl);

    String checkoutUrl = checkoutService.checkoutCart(
        cartRequest);
    if (StringUtils.isBlank(checkoutUrl)) {
      throw new SilFaultException(SilFaults.PAA_SYSTEM_ERROR,
          "Errore durante la creazione del carrello di pagamento");
    }

    return checkoutUrl;
  }

  public String composeDebtPositionsCheckoutUrl(Long organizationId,
      String iuvs, String callbackUrl, String orgFiscalCode, String accessToken) {

    LimitedTokenRequest limitedTokenRequest = LimitedTokenRequest.builder()
        .app(PU_SIL_SCOPE)
        .organizationId(organizationId)
        .resource(CHECKOUT_RESOURCE)
        .resourceId(iuvs)
        .expireInSeconds(24L * 60 * 60)
        .singleUsage(false)
        .build();

    if (callbackUrl != null) {
      Map<String, Object> sessionData = new HashMap<>();
      sessionData.put(CALLBACK_URL_SESSION_DATA_KEY, callbackUrl);
      limitedTokenRequest.setSessionData(sessionData);
    }

    AccessToken limitedToken = authorizationService.requestLimitedToken(
        limitedTokenRequest, accessToken);

    return applicationBaseUrl
        + "/organization/%s/checkout?token=%s".formatted(orgFiscalCode,
            limitedToken.getAccessToken());
  }

  private UserInfoLimitedScope checkUserInfoLimitedScope(UserInfo loggedUser) {
    if (!(loggedUser instanceof UserInfoLimitedScope loggedUserLimitedScope)) {
      throw new AuthorizationDeniedException("Utente non ad uso limitato.");
    }

    if (!CHECKOUT_RESOURCE.equals(
        loggedUserLimitedScope.getResource().getResource())) {
      throw new AuthorizationDeniedException(
          "Utente ad uso limitato non autorizzato per questa risorsa.");
    }

    if (!PU_SIL_SCOPE.equals(loggedUserLimitedScope.getResource().getApp())) {
      throw new AuthorizationDeniedException(
          "Utente ad uso limitato non autorizzato per questo applicativo.");
    }

    return loggedUserLimitedScope;
  }
}
