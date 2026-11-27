package it.gov.pagopa.pu.sil.service.inbound.payments.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.dto.generated.QueryPaymentStatusType;
import it.gov.pagopa.pu.sil.exception.common.BaseBusinessException;
import it.gov.pagopa.pu.sil.exception.common.IllegalStateBusinessException;
import it.gov.pagopa.pu.sil.exception.common.InvalidValueException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.inbound.payments.debtposition.DebtPositionInstallmentFacadeService;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Objects;

@Slf4j
public abstract class AbstractQueryPaymentsService<I, O> {

  private final OrganizationService organizationService;
  private final DebtPositionInstallmentFacadeService debtPositionInstallmentFacadeService;

  protected AbstractQueryPaymentsService(OrganizationService organizationService,
                                         DebtPositionInstallmentFacadeService debtPositionInstallmentFacadeService) {
    this.organizationService = organizationService;
    this.debtPositionInstallmentFacadeService = debtPositionInstallmentFacadeService;
  }

  protected abstract String getOrgIpaCode(I request);

  protected abstract O mapper(List<Pair<DebtPositionDTO, InstallmentDTO>> debtPositionWithInstallmentList, Organization organization, String accessToken, I request);

  protected abstract PaymentStatusRequest validateAndTransformRequest(I request, String orgIpaCode);

  private BaseBusinessException getExceptionForDebtPositionNotFound(QueryPaymentStatusType idType, String message, String silFaultCustomMessage) {
    return switch (idType) {
      case INSTALLMENT_ID -> new InvalidValueException(ErrorCodeConstants.ERROR_CODE_INVALID_ID_SESSION, message, silFaultCustomMessage);
      case IUD -> new InvalidValueException(ErrorCodeConstants.ERROR_CODE_INVALID_IUD, message, silFaultCustomMessage);
      case NOTICE_NUMBER -> new InvalidValueException(ErrorCodeConstants.ERROR_CODE_INVALID_IUV, message, silFaultCustomMessage);
    };
  }

  public O processRequest(I request, UserInfo userInfo, String accessToken) {
    String orgIpaCode = getOrgIpaCode(request);

    AuthorizationService.validateAdminRole(orgIpaCode, userInfo);

    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);
    Organization organization = organizationService.getOrganizationById(organizationId, accessToken)
      .orElse(null);
    if (organization == null || !OrganizationStatus.ACTIVE.equals(organization.getStatus())) {
      throw new InvalidValueException(
        ErrorCodeConstants.ERROR_CODE_INVALID_ORGANIZATION,
        "Invalid or not authorized organization",
        "L'ente non è valido o non è abilitato"
      );
    }

    //validate and transform the request
    PaymentStatusRequest transformedRequest = validateAndTransformRequest(request, orgIpaCode);

    //ideally there could be multiple debt positions involved
    List<Pair<DebtPositionDTO, InstallmentDTO>> debtPositionWithInstallmentList =
      debtPositionInstallmentFacadeService.fetch(transformedRequest, organization, accessToken);

    if(debtPositionWithInstallmentList.isEmpty()){
      throw getExceptionForDebtPositionNotFound(transformedRequest.idType(), "No debt position found", "Nessuna posizione debitoria trovata");
    }

    //debt positions and installments validations
    debtPositionWithInstallmentList.forEach(debtPositionWithInstallment -> {
      //verify debt position is of the expected organization
      if (!debtPositionWithInstallment.getLeft().getOrganizationId().equals(organizationId)) {
        throw getExceptionForDebtPositionNotFound(transformedRequest.idType(), "Debt position not found", "Posizione debitoria non trovata");
      }
      //validate installment is paid
      validateInstallmentStatus(debtPositionWithInstallment.getRight());
    });

    //map and prepare the response
    return mapper(debtPositionWithInstallmentList, organization, accessToken, request);
  }

  protected void validateInstallmentStatus(InstallmentDTO installment) {
    InstallmentStatus status = Objects.equals(installment.getStatus(), InstallmentStatus.TO_SYNC) ? installment.getSyncStatus().getSyncStatusTo() : installment.getStatus();
    //throw fault if installment is not paid
    if(Objects.equals(status, InstallmentStatus.UNPAID)){
      //unpaid
      throw new IllegalStateBusinessException(ErrorCodeConstants.ERROR_CODE_INSTALLMENT_NOT_PAID, "Payment not initiated");
    } else if(Objects.equals(status, InstallmentStatus.EXPIRED)){
      //unpaid
      throw new IllegalStateBusinessException(ErrorCodeConstants.ERROR_CODE_INSTALLMENT_EXPIRED, "Payment expired");
    } else if(!Objects.equals(status, InstallmentStatus.PAID) && !Objects.equals(status, InstallmentStatus.REPORTED)) {
      //any other state
      log.error("Installment with id[{}] has invalid status[{}]", installment.getInstallmentId(), status);
      throw new IllegalStateBusinessException(ErrorCodeConstants.ERROR_CODE_INSTALLMENT_NOT_PAYABLE, "Installment not payable");
    }
  }
}
