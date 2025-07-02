package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
public abstract class AbstractQueryPaymentsService<REQ, RESP> {

  private final OrganizationService organizationService;

  public AbstractQueryPaymentsService(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  protected abstract void validateRequest(REQ request);

  protected abstract List<Pair<DebtPositionDTO, InstallmentDTO>> getDebtPositionsAndInstallments(REQ request, Organization organization, String accessToken);

  protected abstract String getOrgIpaCode(REQ request);

  protected abstract RESP mapper(List<Pair<DebtPositionDTO, InstallmentDTO>> debtPositionWithInstallmentList, Organization organization, String accessToken);

  protected abstract SilFaults getFaultForDebtPositionNotFound();


  public RESP processRequest(REQ request, UserInfo userInfo, String accessToken) {
    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);
    //check if the logged user has the right to call this endpoint
    String orgIpaCode = getOrgIpaCode(request);
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

    //validate the request
    validateRequest(request);

    //ideally there could be multiple debt positions involved
    List<Pair<DebtPositionDTO, InstallmentDTO>> debtPositionWithInstallmentList = getDebtPositionsAndInstallments(request, organization, accessToken);

    if(debtPositionWithInstallmentList.isEmpty()){
      throw new SilFaultException(getFaultForDebtPositionNotFound(), "Nessuna posizione debitoria trovata");
    }

    //debt positions and installments validations
    debtPositionWithInstallmentList.forEach(debtPositionWithInstallment -> {
      //verify debt position is of the expected organization
      if (!debtPositionWithInstallment.getLeft().getOrganizationId().equals(organizationId)) {
        throw new SilFaultException(getFaultForDebtPositionNotFound(), "Posizione debitoria non trovata");
      }
      //validate installment is paid
      validatePaidInstallment(debtPositionWithInstallment.getRight());
    });

    //map and prepare the response
    return mapper(debtPositionWithInstallmentList, organization, accessToken);
  }

  protected InstallmentDTO findInstallmentOfDebtPosition(DebtPositionDTO debtPosition, Predicate<InstallmentDTO> installmentFinderPredicate) {
    return debtPosition.getPaymentOptions().stream()
      .flatMap(po -> po.getInstallments().stream())
      .filter(installmentFinderPredicate)
      .findFirst()
      .orElseThrow(() -> new SilFaultException(getFaultForDebtPositionNotFound(), "Avviso non trovato"));
  }

  private void validatePaidInstallment(InstallmentDTO installment) {
    //throw fault if installment is not paid
    if(Objects.equals(installment.getStatus(), InstallmentStatus.UNPAID)){
      //unpaid
      throw new SilFaultException(SilFaults.PAA_PAGAMENTO_NON_INIZIATO, "Pagamento non effettuato");
    } else if(Objects.equals(installment.getStatus(), InstallmentStatus.PAID) || Objects.equals(installment.getStatus(), InstallmentStatus.REPORTED)) {
      //any other state
      log.error("Installment with id[{}] has invalid status[{}]", installment.getInstallmentId(), installment.getStatus());
      throw new SilFaultException(SilFaults.PAA_ID_SESSION_NON_VALIDO, "ID session non valido");
    }
  }

}
