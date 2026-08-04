package it.gov.pagopa.pu.sil.service.outbound.actualization;

import it.gov.pagopa.actualization.dto.generated.Error;
import it.gov.pagopa.actualization.dto.generated.Payment;
import it.gov.pagopa.actualization.dto.generated.UpdatedPayment;
import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.actualization.ActualizationService;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyActualizationService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import it.gov.pagopa.pu.sil.exception.*;
import it.gov.pagopa.pu.sil.mapper.AmountUpdatesMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.outbound.SilAccessTokenService;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

@Slf4j
@Service
public class ActualizationFacadeService {
  private final OrgSilServiceComponent orgSilServiceComponent;
  private final LegacyActualizationService legacyActualizationService;
  private final it.gov.pagopa.pu.sil.connector.actualization.ActualizationService actualizationService;
  private final SilAccessTokenService silAccessTokenService;
  private final AmountUpdatesMapper amountUpdatesMapper;

  public ActualizationFacadeService(OrgSilServiceComponent orgSilServiceComponent,
                                    LegacyActualizationService legacyActualizationService,
                                    ActualizationService actualizationService,
                                    SilAccessTokenService silAccessTokenService,
                                    AmountUpdatesMapper amountUpdatesMapper) {
    this.orgSilServiceComponent = orgSilServiceComponent;
    this.legacyActualizationService = legacyActualizationService;
    this.actualizationService = actualizationService;
    this.silAccessTokenService = silAccessTokenService;
    this.amountUpdatesMapper = amountUpdatesMapper;
  }

  public ActualizationResultDTO actualize(Long orgSilServiceId, String nav,
                                          UserInfo loggedUser, String accessToken) {
    OrgSilServiceDTO orgSilService = orgSilServiceComponent.getOrgSilServiceById(orgSilServiceId, accessToken)
      .orElseThrow(() -> new IllegalArgumentException("[ORG_SIL_SERVICE_NOT_FOUND] OrgSilService with id %s not found".formatted(orgSilServiceId)));
    AuthorizationService.validateUserForOrganizationId(orgSilService.getOrganizationId(), loggedUser);
    String orgFiscalCode = AuthorizationService.getOrgFiscalCodeFromUserInfo(loggedUser, orgSilService.getOrganizationId());

    if (BooleanUtils.isTrue(orgSilService.getFlagLegacy())) {
      //legacy implementation
      String silAccessToken = silAccessTokenService.getSilAccessToken(orgFiscalCode, nav, loggedUser, orgSilService, accessToken);

      PagamentoAggiornato legacyResult = legacyActualizationService.actualization(
        orgFiscalCode,
        orgSilService,
        loggedUser,
        silAccessToken,
        Pagamento.builder()
          .importoPosizione(Pagamento.ImportoPosizioneEnum.S)
          .numeroAvviso(nav)
          .cfEnteCreditore(orgFiscalCode)
          .build()
      );
      if (legacyResult.getCodice() != null) {
        throw buildException(legacyResult.getCodice().getValue(), legacyResult.getDettaglio());
      }
      return amountUpdatesMapper.pagamentoAggiornato2AmountUpdatesDTO(legacyResult);
    } else {
      //native implementation
      try {
        UpdatedPayment updatedPayment = actualizationService.actualization(
          orgSilService,
          loggedUser,
          accessToken,
          Payment.builder()
            .nav(nav)
            .orgFiscalCode(orgFiscalCode)
            .build()
        );
        return amountUpdatesMapper.updatedPayment2AmountUpdatesDTO(updatedPayment);
      } catch (HttpServerErrorException e) {
        Error errorResponse = e.getResponseBodyAs(Error.class);
        if (errorResponse != null) {
          throw buildException(errorResponse.getCode(), errorResponse.getMessage());
        } else {
          throw new IllegalStateBusinessException(ErrorCodeConstants.ERROR_CODE_ACTUALIZATION_ERROR, "Unexpected error: " + e.getMessage(), e);
        }
      }
    }
  }

  private RuntimeException buildException(String code, String message) {
    return switch (code) {
      case "002" -> new PaymentNotFoundException(message);
      case "003" -> new PaymentNotNotifiedException(message);
      case "004" -> new PaymentInvalidStatusException(message);
      default ->
        new IllegalStateBusinessException(ErrorCodeConstants.ERROR_CODE_ACTUALIZATION_ERROR, "Unexpected error code: " + code + " - " + message);
    };
  }
}
