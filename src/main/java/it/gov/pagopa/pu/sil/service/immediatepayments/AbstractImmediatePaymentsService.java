package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionErrorDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.CheckoutService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.mapper.SessionIdMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.web.client.HttpServerErrorException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractImmediatePaymentsService<I, O> {
  public static final Pattern VALUE_BETWEEN_SQUARE_BRACKETS_PATTERN = Pattern.compile("^\\[(.*?)\\]\\s*(.*)$");

  protected final CheckoutService checkoutService;
  protected final InstantPaymentsFacade instantPaymentsFacade;
  protected final CartRequestMapper cartRequestMapper;
  protected final OrganizationService organizationService;
  protected final SessionIdMapper sessionIdMapper;

  protected AbstractImmediatePaymentsService(CheckoutService checkoutService,
                                             InstantPaymentsFacade instantPaymentsFacade,
                                             OrganizationService organizationService,
                                             CartRequestMapper cartRequestMapper,
                                             SessionIdMapper sessionIdMapper) {
    this.checkoutService = checkoutService;
    this.instantPaymentsFacade = instantPaymentsFacade;
    this.cartRequestMapper = cartRequestMapper;
    this.organizationService = organizationService;
    this.sessionIdMapper = sessionIdMapper;
  }

  protected abstract List<DebtPositionDTO> createDebtPositionsFromMapping(PaymentRequestMappingResult paymentRequestMappingResult, String accessToken);
  // NOTE: changed to return a PaymentRequestMappingResult which may contain either plain debt positions or mixed debt positions
  protected abstract PaymentRequestMappingResult mapRequestToDebtPositions(I request, Organization org, String cartId, String accessToken);

  protected abstract O mapToResponse(String outcome, String checkoutUrl, String sessionId);

  protected abstract String getCallbackUrl(I request);

  public Triple<O, String, RegistryOutcome> processRequest(I request, String orgIpaCode, UserInfo userInfo, String accessToken) {
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
    if (StringUtils.isNotBlank(getCallbackUrl(request)) && !ValidationUtils.isValidUri(getCallbackUrl(request))) {
      throw new SilFaultException(SilFaults.PAA_URL_NON_VALIDA, "URL di callback non valida");
    }

    String cartId = UUID.randomUUID().toString();

    //map request to debt positions and validate it
    PaymentRequestMappingResult paymentRequestMappingResult = mapRequestToDebtPositions(request, organization, cartId, accessToken);

    //create debt positions using the facade which decides between mixed and plain
    List<DebtPositionDTO> debtPositions = createDebtPositionsFromMapping(paymentRequestMappingResult, accessToken);

    String iuvs = debtPositions.stream()
      .flatMap(dp -> dp.getPaymentOptions().stream())
      .flatMap(option -> option.getInstallments().stream())
      .map(InstallmentDTO::getIuv)
      .collect(Collectors.joining(Utilities.IUV_SEPARATOR));

    //sessionId is a concatenation of all installment IDs, used to track the session
    String sessionId = sessionIdMapper.mapDebtPositionsToSessionId(debtPositions);

    //map debt positions to cart request
    CartRequest cartRequest = cartRequestMapper.mapDebtPositionsToCartRequest(debtPositions, organization, cartId, getCallbackUrl(request));

    //invoke carts API to trigger the payment on Checkout
    String checkoutUrl = checkoutService.checkoutCart(cartRequest);
    if(StringUtils.isBlank(checkoutUrl)){
      throw new SilFaultException(SilFaults.PAA_SYSTEM_ERROR, "Errore durante la creazione del carrello di pagamento");
    }

    O response = mapToResponse(RegistryOutcome.OK.getValue(), checkoutUrl, sessionId);
    return Triple.of(response, iuvs, RegistryOutcome.OK);
  }

  protected SilFaultException buildException(HttpServerErrorException e) {
    DebtPositionErrorDTO errorResponse = e.getResponseBodyAs(DebtPositionErrorDTO.class);
    if (errorResponse == null) {
      return new SilFaultException(SilFaults.PAA_SYSTEM_ERROR, "errore durante la creazione delle posizioni debitorie");
    }
    Matcher matcher = VALUE_BETWEEN_SQUARE_BRACKETS_PATTERN.matcher(errorResponse.getMessage());
    if (matcher.matches()) {
      return new SilFaultException(
        SilFaults.fromNativeFault2LegacyCode(matcher.group(1)),
        matcher.group(2)
      );
    }
    return new SilFaultException(SilFaults.PAA_SYSTEM_ERROR, errorResponse.getMessage());
  }
}
