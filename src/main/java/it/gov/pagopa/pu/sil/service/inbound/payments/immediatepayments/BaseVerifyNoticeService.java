package it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.inbound.payments.debtposition.DebtPositionCheckoutService;
import it.gov.pagopa.pu.sil.service.inbound.payments.debtposition.InstallmentFacadeService;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class BaseVerifyNoticeService<I, O> {

  protected final OrganizationService organizationService;
  protected final InstallmentFacadeService installmentFacadeService;
  protected final DebtPositionCheckoutService debtPositionCheckoutService;

  protected BaseVerifyNoticeService(OrganizationService organizationService,
                                    InstallmentFacadeService installmentFacadeService,
                                    DebtPositionCheckoutService debtPositionCheckoutService) {
    this.organizationService = organizationService;
    this.installmentFacadeService = installmentFacadeService;
    this.debtPositionCheckoutService = debtPositionCheckoutService;
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

    String iuv = getNav(request);
    if (StringUtils.isBlank(iuv)) {
      throw new SilFaultException(SilFaults.PAA_IUV_NON_VALIDO, "Identificativo univoco del versamento non indicato");
    }

    List<InstallmentDTO> installments = installmentFacadeService.getInstallmentsByOrganizationIdAndNav(organizationId, iuv, accessToken);

    InstallmentDTO installmentDTO = installments.stream()
      .findFirst().orElseThrow(() -> new SilFaultException(SilFaults.PAA_IUV_NON_VALIDO, "Nessun avviso pagabile trovato per l'identificativo univoco del versamento indicato"));

    return handleInstallmentsStatus(installmentDTO, organization, callbackUrl, accessToken);
  }

  protected abstract String getNav(I request);
  protected abstract String getCallbackUrl(I request);
  protected abstract O mapToResponse(String outcome, String checkoutUrl, String sessionId);
  protected abstract O handleInstallmentsStatus(InstallmentDTO installment, Organization organization, String callbackUrl, String accessToken);

  protected O doCheckOut(InstallmentDTO payableInstallment,
    Organization organization, String callbackUrl, String accessToken) {
    String sessionId = String.valueOf(payableInstallment.getInstallmentId());
    String checkoutUrl = debtPositionCheckoutService.composeDebtPositionsCheckoutUrl(
      organization.getOrganizationId(), payableInstallment.getIuv(),
      callbackUrl, organization.getOrgFiscalCode(), accessToken);

    return mapToResponse(RegistryOutcome.OK.getValue(), checkoutUrl, sessionId);
  }
}
