package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.PaidExportFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.legacy.ExportFileLegacyFileVersion;
import it.gov.pagopa.pu.sil.exception.ExportFileRequestValidationException;
import it.gov.pagopa.pu.sil.exception.UnauthorizedException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.veneto.regione.pagamenti.ente.PaaSILPrenotaExportFlusso;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaaSILPrenotaExportFlussoService {

  private final ExportFileService exportFileService;
  private final DebtPositionService debtPositionService;
  private final ValidationService validationService;

  public PaaSILPrenotaExportFlussoService(ExportFileService exportFileService,
                                          DebtPositionService debtPositionService,
                                          ValidationService validationService) {
    this.exportFileService = exportFileService;
    this.debtPositionService = debtPositionService;
    this.validationService = validationService;
  }

  public Long paaSILPrenotaExportFlusso(
    UserInfo userInfo,
    String accessToken,
    String orgIpaCode,
    PaaSILPrenotaExportFlusso request) {

    String clientId = Optional.ofNullable(userInfo).map(UserInfo::getUserId).orElse(null);

    if (!AuthorizationService.isAdminRole(orgIpaCode, userInfo)) {
      log.error("ClientId [{}] not authorized to call export file for organization {}", clientId, orgIpaCode);
      throw new UnauthorizedException("Utente non autorizzato");
    }

    Long organizationId = AuthorizationService.getOrganizationIdFromUserInfo(userInfo, orgIpaCode);

    Optional<PaaSILPrenotaExportFlusso> optRequest = Optional.ofNullable(request);

    Set<String> validFileVersionValues = Arrays.stream(ExportFileLegacyFileVersion.values())
      .map(ExportFileLegacyFileVersion::getLegacyValue).collect(Collectors.toSet());
    String fileVersion = optRequest.map(PaaSILPrenotaExportFlusso::getVersioneTracciato)
      .filter(validFileVersionValues::contains)
      .orElseThrow(() -> new ExportFileRequestValidationException(SilFaults.PAA_VERSIONE_TRACCIATO_NON_VALIDA));

    OffsetDateTime from = optRequest.flatMap(r -> Optional.ofNullable(r.getDateFrom()))
      .map(x -> x.toGregorianCalendar().toZonedDateTime().toOffsetDateTime())
      .orElseThrow(() -> new ExportFileRequestValidationException(SilFaults.PAA_DATE_FROM_NON_VALIDO));

    OffsetDateTime to = optRequest.flatMap(r -> Optional.ofNullable(r.getDateTo()))
      .map(x -> x.toGregorianCalendar().toZonedDateTime().toOffsetDateTime())
      .orElseThrow(() -> new ExportFileRequestValidationException(SilFaults.PAA_DATE_TO_NON_VALIDO));

    if (to.isBefore(from)) {
      throw new ExportFileRequestValidationException(SilFaults.PAA_INTERVALLO_DATE_NON_VALIDO);
    }

    Long debtPositionTypeOrgId = optRequest.flatMap(r -> Optional.ofNullable(r.getIdentificativoTipoDovuto()))
      .map(debtPositionTypeOrgCode -> {
        DebtPositionTypeOrg debtPositionTypeOrg = debtPositionService.getDebtPositionTypeOrgByOrgIdAndType(
          organizationId, debtPositionTypeOrgCode, accessToken);

        Pair<SilFaults, String> fault = validationService.validateDebtPositionTypeOrg(debtPositionTypeOrg, debtPositionTypeOrgCode);

        if (fault != null) {
          log.error("Validation failed for debt position type: {}", fault.getRight());
          throw new ExportFileRequestValidationException(fault.getLeft());
        }

        return debtPositionTypeOrgCode;
      })
      .map(Long::valueOf)
      .orElseThrow(() -> new ExportFileRequestValidationException(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO));

    PaidExportFileRequestDTO requestDTO = mapToExportRequest(organizationId, fileVersion, from, to, debtPositionTypeOrgId);

    Long exportFileId = exportFileService.createPaidExportFile(requestDTO, accessToken);
    log.debug("Export file created with ID: {}", exportFileId);

    return exportFileId;
  }

  private PaidExportFileRequestDTO mapToExportRequest(Long organizationId,
                                                      String fileVersion,
                                                      OffsetDateTime from,
                                                      OffsetDateTime to,
                                                      Long debtPositionTypeOrgId) {
    return new PaidExportFileRequestDTO()
      .organizationId(organizationId)
      .exportFileType(PaidExportFileRequestDTO.ExportFileTypeEnum.PAID)
      .fileVersion(fileVersion)
      .filterFields(new PaidExportFileFilter()
        .paymentDateTime(new OffsetDateTimeIntervalFilter()
          .from(from)
          .to(to))
        .debtPositionTypeOrgId(debtPositionTypeOrgId));
  }
}
