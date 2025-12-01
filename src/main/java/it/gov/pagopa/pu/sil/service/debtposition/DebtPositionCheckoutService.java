package it.gov.pagopa.pu.sil.service.debtposition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
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
import it.gov.pagopa.pu.sil.util.Utilities;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;

@Service
public class DebtPositionCheckoutService {
  public static final String PU_SIL_SCOPE = "pu-sil";
  public static final String CHECKOUT_RESOURCE = "CHECKOUT";

  private final OrganizationService organizationService;
  private final DebtPositionService debtPositionService;
  private final CheckoutService checkoutService;
  private final CartRequestMapper cartRequestMapper;

  private final ObjectMapper objectMapper;

  public DebtPositionCheckoutService(OrganizationService organizationService,
    DebtPositionService debtPositionService, CheckoutService checkoutService,
    CartRequestMapper cartRequestMapper, ObjectMapper objectMapper) {
    this.organizationService = organizationService;
    this.debtPositionService = debtPositionService;
    this.checkoutService = checkoutService;
    this.cartRequestMapper = cartRequestMapper;
    this.objectMapper = objectMapper;
  }

  public void redirectToCheckout(UserInfo loggedUser,
    String orgIpaCode, String accessToken) {
    UserInfoLimitedScope loggedUserLimitedScope = checkUserInfoLimitedScope(
      loggedUser);
    checkAccessTokenScope(accessToken);

    Optional<Organization> optionalOrganization = organizationService.getOrganizationByIpaCode(
      orgIpaCode, accessToken);

    if (optionalOrganization.isEmpty()) {
      throw new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO);
    }

    Organization organization = optionalOrganization.get();

    List<String> iuvs = Arrays.stream(
      loggedUserLimitedScope.getResource().getResourceId().split(Utilities.IUV_SEPARATOR)).toList();

    List<DebtPositionDTO> debtPositions = iuvs.stream().map(
        iuv -> debtPositionService.getDebtPositionsByOrganizationIdAndIuv(
          organization.getOrganizationId(), iuv,
          List.of(DebtPositionOrigin.SPONTANEOUS_SIL), accessToken))
      .flatMap(List::stream).toList();

    String cartId = UUID.randomUUID().toString();
    CartRequest cartRequest = cartRequestMapper.mapDebtPositionsToCartRequest(
      debtPositions, organization, cartId, null); // @TODO: implementare callbackUrl con P4ADEV-4283

    String checkoutUrl = checkoutService.checkoutCart(
      cartRequest); // @TODO: da implementare con la P4ADEV-4043
    if (StringUtils.isBlank(checkoutUrl)) {
      throw new SilFaultException(SilFaults.PAA_SYSTEM_ERROR,
        "Errore durante la creazione del carrello di pagamento");
    }
  }

  private UserInfoLimitedScope checkUserInfoLimitedScope(UserInfo loggedUser) {
    if (!(loggedUser instanceof UserInfoLimitedScope loggedUserLimitedScope)) {
      throw new AuthorizationDeniedException(
        "Utente non ad uso limitato.");
    }

    if (!CHECKOUT_RESOURCE.equals(
      loggedUserLimitedScope.getResource().getResource())) {
      throw new AuthorizationDeniedException(
        "Utente ad uso limitato non autorizzato.");
    }

    return loggedUserLimitedScope;
  }

  private void checkAccessTokenScope(String accessToken) {
    try {
      String[] chunks = accessToken.split("\\.");
      Base64.Decoder decoder = Base64.getUrlDecoder();
      String payloadString = new String(decoder.decode(chunks[1]),
        StandardCharsets.UTF_8);
      JsonNode payload = objectMapper.readTree(payloadString);
      JsonNode scopeNode = payload.get("scope");
      String scope = scopeNode.asText();

      if (!(PU_SIL_SCOPE.equals(scope))) {
        throw new AuthorizationDeniedException(
          "Utente ad uso limitato non autorizzato.");
      }
    } catch (JsonProcessingException e) {
      throw new SilFaultException(SilFaults.PAA_SYSTEM_ERROR,
        "Qualcosa è andato storto nella lettura del token di accesso.");
    }
  }
}
