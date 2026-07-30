package it.gov.pagopa.pu.sil.service.singleimport;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.ManageDebtPositionDTO;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.IllegalStateBusinessException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.debtposition.ManageDebtPositionService;
import it.gov.pagopa.pu.sil.service.notice.NoticeService;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import jakarta.activation.DataHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Slf4j
public abstract class BaseDebtPositionHandler<I, O> {

  private final OrganizationService organizationService;
  private final DebtPositionService debtPositionService;
  private final ManageDebtPositionService manageDebtPositionService;
  private final NoticeService noticeService;

  protected BaseDebtPositionHandler(OrganizationService organizationService,
                                 DebtPositionService debtPositionService,
                                 ManageDebtPositionService manageDebtPositionService,
                                 NoticeService noticeService) {
    this.organizationService = organizationService;
    this.debtPositionService = debtPositionService;
    this.manageDebtPositionService = manageDebtPositionService;
    this.noticeService = noticeService;
  }
  protected abstract Pair<DebtPositionDTO, String> mapRequestToDebtPosition(I request, Organization organization, String accessToken);
  protected abstract O mapToRespone(DebtPositionDTO debtPosition, String orgFiscalCode, DataHandler notice, String action);
  protected abstract ManageDebtPositionDTO mapToManageDebtPositionDTO(DebtPositionDTO debtPositionOnDb, DebtPositionDTO debtPositionToSync, String action);

  public Triple<O, String, RegistryOutcome> handleAction(I request, String orgIpaCode, UserInfo userInfo, String accessToken) {

    AuthorizationService.validateAdminRole(orgIpaCode, userInfo);
    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);
    Organization organization = organizationService.getOrganizationById(organizationId, accessToken)
      .orElse(null);
    if (organization == null || !OrganizationStatus.ACTIVE.equals(organization.getStatus())) {
      throw new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "L'ente non è valido o non è abilitato");
    }

    Pair<DebtPositionDTO, String> debtPositionWithAction = mapRequestToDebtPosition(request, organization, accessToken);

    return switch (debtPositionWithAction.getRight()) {
      case Constants.LEGACY_IMPORT_ACTION_INSERT -> handleInsert(debtPositionWithAction.getRight(), debtPositionWithAction.getLeft(), organization.getOrgFiscalCode(), accessToken);
      case Constants.LEGACY_IMPORT_ACTION_MODIFY,
           Constants.LEGACY_IMPORT_ACTION_CANCEL ->
        handleModifyAndCancel(debtPositionWithAction.getRight(), debtPositionWithAction.getLeft(), organization, accessToken);
      case Constants.LEGACY_IMPORT_ACTION_PRINT ->
        handlePrintNotice(debtPositionWithAction.getRight(), debtPositionWithAction.getLeft(), organization, accessToken);
      default ->
        throw new SilFaultException(SilFaults.PAA_AZIONE_NON_VALIDA, "Azione non valida: " + debtPositionWithAction.getRight());
    };
  }

  private Triple<O, String, RegistryOutcome> handleInsert(String action, DebtPositionDTO debtPosition, String orgFiscalCode, String accessToken) {
    //create debt position (and wait for the workflow to complete)
    DebtPositionDTO debtPositionDTO = manageDebtPositionService.createDebtPositions(List.of(debtPosition), accessToken).getFirst();
    //retrieve the IUV
    String iuv = debtPositionDTO.getPaymentOptions().getFirst().getInstallments().getFirst().getIuv();
    //prepare the response
    O response = mapToRespone(debtPositionDTO, orgFiscalCode, null, action);

    return Triple.of(response, iuv, RegistryOutcome.OK);
  }

  private Triple<O, String, RegistryOutcome> handleModifyAndCancel(String action, DebtPositionDTO debtPositionToSync, Organization organization, String accessToken) {

    //for modify and cancel we always have only one installment to sync (the one with the IUD passed in input)
    InstallmentDTO installmentToSync = debtPositionToSync.getPaymentOptions().getFirst().getInstallments().getFirst();
    String iud = installmentToSync.getIud();

    //find the existing debt position on db
    Pair<DebtPositionDTO, InstallmentDTO> debtPositionWithInstallment = findSyncableDebtPositionByIud(organization.getOrganizationId(), iud, accessToken);
    DebtPositionDTO debtPositionOnDb = debtPositionWithInstallment.getLeft();
    InstallmentDTO installmentOnDb = debtPositionWithInstallment.getRight();

    //map to InstallmentSynchronizeDTO
    ManageDebtPositionDTO manageDebtPositionDTO = mapToManageDebtPositionDTO(debtPositionOnDb, debtPositionToSync, action);

    //execute the action on the installment
    log.info("executing action {} on installment with iud[{}] for organizationId[{}]", action, iud, organization.getOrganizationId());
    DebtPositionDTO debtPositionDTO = manageDebtPositionService.manageDebtPositionInstallments(debtPositionOnDb.getDebtPositionId(), manageDebtPositionDTO, accessToken);

    O response = mapToRespone(debtPositionDTO, organization.getOrgFiscalCode(), null, action);

    return Triple.of(response, installmentOnDb.getIuv(), RegistryOutcome.OK);
  }

  private Triple<O, String, RegistryOutcome> handlePrintNotice(String action, DebtPositionDTO debtPosition, Organization organization, String accessToken) {
    String iud = debtPosition.getPaymentOptions().getFirst().getInstallments().getFirst().getIud();
    //find the existing debt position
    Pair<DebtPositionDTO, InstallmentDTO> debtPositionWithInstallment = findSyncableDebtPositionByIud(organization.getOrganizationId(), iud, accessToken);
    DebtPositionDTO debtPositionOnDb = debtPositionWithInstallment.getLeft();
    InstallmentDTO installmentOnDb = debtPositionWithInstallment.getRight();

    //generate the notice
    byte[] noticeAsBytes = noticeService.generateNotice(installmentOnDb.getNav(), debtPositionOnDb, accessToken);

    //compress the notice into a zip file
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ZipOutputStream zos = new ZipOutputStream(baos)) {
      String name = "avviso_" + organization.getOrgFiscalCode() + "_" + installmentOnDb.getIuv() + ".pdf";
      ZipEntry entry = new ZipEntry(name);
      zos.putNextEntry(entry);
      zos.write(noticeAsBytes);
      zos.closeEntry();
      zos.finish();
      DataHandler avviso = new DataHandler(new ByteArrayDataSource("application/octet-stream", baos.toByteArray()));

      O response = mapToRespone(debtPositionOnDb, organization.getOrgFiscalCode(), avviso, action);

      return Triple.of(response, installmentOnDb.getIuv(), RegistryOutcome.OK);
    } catch (Exception e) {
      throw new IllegalStateBusinessException(
        ErrorCodeConstants.ERROR_CODE_GENERATE_NOTICE_ERROR,
        "Error generating zip for notice org[%s] iud[%s]".formatted(organization.getOrganizationId(), iud),
        e);
    }
  }

  private Pair<DebtPositionDTO, InstallmentDTO> findSyncableDebtPositionByIud(Long organizationId, String iud, String accessToken) {
    DebtPositionDTO debtPositionOnDb = debtPositionService.getDebtPositionsByOrganizationIdAndIud(
        organizationId, iud, Constants.ORDINARY_DEBT_POSITION_ORIGINS, accessToken).stream()
      .filter(dp -> Constants.SYNCABLE_DEBT_POSITION_STATUSES.contains(dp.getStatus()))
      .findFirst()
      .orElseThrow(() -> {
        log.error("Debt position not found for organizationId[{}] and iud[{}]", organizationId, iud);
        return new SilFaultException(SilFaults.PAA_IMPORT_DOVUTO_NON_PRESENTE, "Dovuto non trovato");
      });
    InstallmentDTO installmentOnDb = debtPositionOnDb.getPaymentOptions().stream()
      .flatMap(po -> po.getInstallments().stream())
      .filter(i -> Objects.equals(i.getIud(), iud))
      .findFirst()
      .orElseThrow(() -> {
        log.error("Installment not found for organizationId[{}] and iud[{}]", organizationId, iud);
        return new SilFaultException(SilFaults.PAA_IMPORT_DOVUTO_NON_PRESENTE, "Dovuto non trovato");
      });
    return Pair.of(debtPositionOnDb, installmentOnDb);
  }
}
