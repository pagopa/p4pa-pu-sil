package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.CheckoutService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.mapper.PaaSILInviaDovutiMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.debtposition.CreateDebtPositionService;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.gov.pagopa.pu.sil.util.soap.FaultUtils;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovutiRisposta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaaSILInviaDovutiService {

  private final PaaSILInviaDovutiMapper paaSILInviaDovutiMapper;
  private final CheckoutService checkoutService;
  private final CreateDebtPositionService createDebtPositionService;
  private final CartRequestMapper cartRequestMapper;

  public Triple<PaaSILInviaDovutiRisposta, String, RegistryOutcome> paaSILInviaDovuti(PaaSILInviaDovuti request, String orgIpaCode, UserInfo userInfo, String accessToken) {
    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);
    //check if the logged user has the right to call this endpoint
    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call paaSILInviaDovuti for organization {}", clientId, orgIpaCode);
      return setFaultResponse(SilFaults.PAA_ENTE_NON_VALIDO, "Utente non autorizzato");
    }

    //validate callback URL
    if (StringUtils.isNotBlank(request.getEnteSILInviaRispostaPagamentoUrl()) && !ValidationUtils.isValidUri(request.getEnteSILInviaRispostaPagamentoUrl())) {
      return setFaultResponse(SilFaults.PAA_URL_NON_VALIDA, "URL di callback non valida");
    }

    String cartId = UUID.randomUUID().toString();

    //map request to debt positions and validate it
    List<DebtPositionDTO> mappedRequest = paaSILInviaDovutiMapper.mapRequestToDebtPositions(request, cartId, userInfo, orgIpaCode, accessToken);

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
    CartRequest cartRequest = cartRequestMapper.mapDebtPositionsToCartRequest(debtPositions, cartId, request.getEnteSILInviaRispostaPagamentoUrl());

    //invoke carts API to trigger the payment on Checkout
    String checkoutUrl = checkoutService.checkoutCart(cartRequest);

    PaaSILInviaDovutiRisposta response = new PaaSILInviaDovutiRisposta();
    response.setEsito(RegistryOutcome.OK.getValue());
    response.setUrl(checkoutUrl);
    response.setIdSession(sessionId);
    response.setRedirect(1); // 1 means redirect to the checkout URL
    return Triple.of(response, iuvs, RegistryOutcome.OK);
  }

  private Triple<PaaSILInviaDovutiRisposta, String, RegistryOutcome> setFaultResponse(SilFaults fault, String description) {
    PaaSILInviaDovutiRisposta response = new PaaSILInviaDovutiRisposta();
    response.setEsito(RegistryOutcome.KO.getValue());
    return Triple.of(FaultUtils.setFaultOnResponse(response, fault, description), null, RegistryOutcome.KO);
  }
}
