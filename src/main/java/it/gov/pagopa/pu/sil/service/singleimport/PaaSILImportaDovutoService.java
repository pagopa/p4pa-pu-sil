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
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.PaaSILImportaDovutoMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.debtposition.ManageDebtPositionService;
import it.gov.pagopa.pu.sil.service.notice.NoticeService;
import it.gov.pagopa.pu.sil.util.ByteArrayDataSource;
import it.gov.pagopa.pu.sil.util.Constants;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovuto;
import it.veneto.regione.pagamenti.ente.PaaSILImportaDovutoRisposta;
import jakarta.activation.DataHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class PaaSILImportaDovutoService {

    private final OrganizationService organizationService;
    private final PaaSILImportaDovutoMapper paaSILImportaDovutoMapper;
    private final DebtPositionService debtPositionService;
    private final ManageDebtPositionService manageDebtPositionService;
    private final NoticeService noticeService;
    private final String puSilBaseUrl;

    public PaaSILImportaDovutoService(OrganizationService organizationService,
                                      PaaSILImportaDovutoMapper paaSILImportaDovutoMapper,
                                      DebtPositionService debtPositionService,
                                      ManageDebtPositionService manageDebtPositionService,
                                      NoticeService noticeService,
                                      @Value("${public-base-url.pu-sil}") String puSilBaseUrl) {
        this.organizationService = organizationService;
        this.paaSILImportaDovutoMapper = paaSILImportaDovutoMapper;
        this.debtPositionService = debtPositionService;
        this.manageDebtPositionService = manageDebtPositionService;
        this.noticeService = noticeService;
        this.puSilBaseUrl = puSilBaseUrl;
    }


    public Triple<PaaSILImportaDovutoRisposta, String, RegistryOutcome> paaSILImportaDovuto(PaaSILImportaDovuto request, String orgIpaCode, UserInfo userInfo, String accessToken) {

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

        Pair<DebtPositionDTO, String> debtPositionWithAction = paaSILImportaDovutoMapper.mapRequestToDebtPosition(request, organization, accessToken);
        PaaSILImportaDovutoRisposta response = switch (debtPositionWithAction.getRight()) {
            case Constants.LEGACY_IMPORT_ACTION_INSERT -> handleInsert(debtPositionWithAction.getLeft(), organization.getOrgFiscalCode(), accessToken);
            case Constants.LEGACY_IMPORT_ACTION_MODIFY,
                 Constants.LEGACY_IMPORT_ACTION_CANCEL ->
                    handleModifyAndCancel(debtPositionWithAction.getRight(), debtPositionWithAction.getLeft(), organization, accessToken);
            case Constants.LEGACY_IMPORT_ACTION_PRINT ->
                    handlePrintNotice(debtPositionWithAction.getLeft(), organization, accessToken);
            default ->
                    throw new SilFaultException(SilFaults.PAA_AZIONE_NON_VALIDA, "Azione non valida: " + debtPositionWithAction.getRight());
        };

        return Triple.of(response, response.getIdentificativoUnivocoVersamento(), RegistryOutcome.OK);
    }

    private PaaSILImportaDovutoRisposta handleInsert(DebtPositionDTO debtPosition, String orgFiscalCode, String accessToken) {
        //create debt position (and wait for the workflow to complete)
        DebtPositionDTO debtPositionDTO = manageDebtPositionService.createSyncedDebtPositions(List.of(debtPosition), accessToken).getFirst();
        //retrieve the IUV
        String iuv = debtPositionDTO.getPaymentOptions().getFirst().getInstallments().getFirst().getIuv();
        //prepare the response
        PaaSILImportaDovutoRisposta response = new PaaSILImportaDovutoRisposta();
        response.setEsito(RegistryOutcome.OK.getValue());
        response.setIdentificativoUnivocoVersamento(debtPositionDTO.getPaymentOptions().getFirst().getInstallments().getFirst().getIuv());
        response.setUrlFileAvviso(composeDownloadPdfNoticeUrl(orgFiscalCode, iuv));
        return response;
    }

    private PaaSILImportaDovutoRisposta handleModifyAndCancel(String action, DebtPositionDTO debtPositionToSync, Organization organization, String accessToken) {
        InstallmentDTO installmentToSync = debtPositionToSync.getPaymentOptions().getFirst().getInstallments().getFirst();
        String iud = installmentToSync.getIud();

        //find the existing debt position on db
        Pair<DebtPositionDTO, InstallmentDTO> debtPositionWithInstallment = findSyncableDebtPositionByIud(organization.getOrganizationId(), iud, accessToken);
        DebtPositionDTO debtPositionOnDb = debtPositionWithInstallment.getLeft();
        InstallmentDTO installmentOnDb = debtPositionWithInstallment.getRight();

        //map to InstallmentSynchronizeDTO
        ManageDebtPositionDTO manageDebtPositionDTO = paaSILImportaDovutoMapper.mapToManageDebtPositionDTO(debtPositionOnDb, installmentToSync, action);

        //execute the action on the installment
        log.info("executing action {} on installment with iud[{}] for organizationId[{}]", action, iud, organization.getOrganizationId());
        manageDebtPositionService.manageDebtPositionInstallments(debtPositionOnDb.getDebtPositionId(), manageDebtPositionDTO, accessToken);

        PaaSILImportaDovutoRisposta response = new PaaSILImportaDovutoRisposta();
        response.setEsito(RegistryOutcome.OK.getValue());
        response.setIdentificativoUnivocoVersamento(installmentOnDb.getIuv());
        if (action.equals(Constants.LEGACY_IMPORT_ACTION_MODIFY)) {
            response.setUrlFileAvviso(composeDownloadPdfNoticeUrl(organization.getOrgFiscalCode(), installmentOnDb.getIuv()));
        }
        return response;
    }

    private PaaSILImportaDovutoRisposta handlePrintNotice(DebtPositionDTO debtPosition, Organization organization, String accessToken) {
        String iud = debtPosition.getPaymentOptions().getFirst().getInstallments().getFirst().getIud();
        //find the existing debt position
        Pair<DebtPositionDTO, InstallmentDTO> debtPositionWithInstallment = findSyncableDebtPositionByIud(organization.getOrganizationId(), iud, accessToken);
        DebtPositionDTO debtPositionOnDb = debtPositionWithInstallment.getLeft();
        InstallmentDTO installmentOnDb = debtPositionWithInstallment.getRight();

        //generate the notice
        byte[] noticeAsBytes = noticeService.generateNotice(installmentOnDb.getIuv(), debtPositionOnDb, accessToken);

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

            PaaSILImportaDovutoRisposta response = new PaaSILImportaDovutoRisposta();
            response.setEsito(RegistryOutcome.OK.getValue());
            response.setIdentificativoUnivocoVersamento(installmentOnDb.getIuv());
            response.setBase64ZipAvviso(avviso);
            return response;
        } catch (Exception e) {
            log.error("Error generating zip for notice org[{}] iud[{}]", organization.getOrganizationId(), iud, e);
            throw new ApplicationException(e);
        }
    }

    private String composeDownloadPdfNoticeUrl(String orgFiscalCode, String iuv) {
        return UriComponentsBuilder.fromUriString(puSilBaseUrl)
                .path("/sil/organization/{orgFiscalCode}/printpaymentnotice/{iuv}")
                .buildAndExpand(orgFiscalCode, iuv)
                .toUriString();
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


