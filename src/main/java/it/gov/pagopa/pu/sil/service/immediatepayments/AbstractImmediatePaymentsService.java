package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.CheckoutService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.debtposition.CreateDebtPositionService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractImmediatePaymentsService<REQ, RESP> {
  protected final CheckoutService checkoutService;
  protected final CreateDebtPositionService createDebtPositionService;
  protected final CartRequestMapper cartRequestMapper;

  protected AbstractImmediatePaymentsService(CheckoutService checkoutService, CreateDebtPositionService createDebtPositionService, CartRequestMapper cartRequestMapper) {
    this.checkoutService = checkoutService;
    this.createDebtPositionService = createDebtPositionService;
    this.cartRequestMapper = cartRequestMapper;
  }

  protected abstract List<DebtPositionDTO> mapRequestToDebtPositions(REQ request, String cartId, UserInfo userInfo, String orgIpaCode, String accessToken);

  protected abstract RESP mapToResponse(String outcome, String checkoutUrl, String sessionId);

  protected abstract String getCallbackUrl(REQ request);

  public Triple<RESP, String, RegistryOutcome> processRequest(REQ request, String orgIpaCode, UserInfo userInfo, String accessToken) {
    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);
    //check if the logged user has the right to call this endpoint
    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call {} for organization {}", request.getClass().getSimpleName(), clientId, orgIpaCode);
      throw new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "Utente non autorizzato");
    }

    //validate callback URL
    if (StringUtils.isNotBlank(getCallbackUrl(request)) && !ValidationUtils.isValidUri(getCallbackUrl(request))) {
      throw new SilFaultException(SilFaults.PAA_URL_NON_VALIDA, "URL di callback non valida");
    }

    String cartId = UUID.randomUUID().toString();

    //map request to debt positions and validate it
    List<DebtPositionDTO> mappedRequest = mapRequestToDebtPositions(request, cartId, userInfo, orgIpaCode, accessToken);

    //create debt positions
    List<DebtPositionDTO> debtPositions = createDebtPositionService.createSyncedDebtPositions(mappedRequest, accessToken);

    String iuvs = debtPositions.stream()
      .flatMap(dp -> dp.getPaymentOptions().stream())
      .flatMap(option -> option.getInstallments().stream())
      .map(InstallmentDTO::getIuv)
      .collect(Collectors.joining(Utilities.IUV_SEPARATOR));

    //sessionId is a concatenation of all debt position IDs, used to track the session
    String sessionId = debtPositions.stream()
      .map(DebtPositionDTO::getDebtPositionId)
      .map(String::valueOf)
      .collect(Collectors.joining(Constants.SESSION_ID_SEPARATOR));

    //map debt positions to cart request
    CartRequest cartRequest = cartRequestMapper.mapDebtPositionsToCartRequest(debtPositions, cartId, getCallbackUrl(request));

    //invoke carts API to trigger the payment on Checkout
    String checkoutUrl = checkoutService.checkoutCart(cartRequest);

    RESP response = mapToResponse(RegistryOutcome.OK.getValue(), checkoutUrl, sessionId);
    return Triple.of(response, iuvs, RegistryOutcome.OK);
  }
}
