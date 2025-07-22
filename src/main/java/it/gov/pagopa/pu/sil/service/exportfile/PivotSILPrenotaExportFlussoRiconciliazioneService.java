package it.gov.pagopa.pu.sil.service.exportfile;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.processexecutions.ExportFileService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.mapper.ClassificationsExportFileRequestMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILPrenotaExportFlussoRiconciliazione;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILPrenotaExportFlussoRiconciliazioneRisposta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
public class PivotSILPrenotaExportFlussoRiconciliazioneService extends AbstractExportFileReservationService {
  private final ExportFileService exportFileService;
  private final ClassificationsExportFileRequestMapper classificationsExportFileRequestMapper;

  public PivotSILPrenotaExportFlussoRiconciliazioneService(ExportFileService exportFileService,
                                                           DebtPositionTypeService debtPositionTypeService,
                                                           ClassificationsExportFileRequestMapper classificationsExportFileRequestMapper) {
    super(debtPositionTypeService);
    this.exportFileService = exportFileService;
    this.classificationsExportFileRequestMapper = classificationsExportFileRequestMapper;
  }

  public PivotSILPrenotaExportFlussoRiconciliazioneRisposta doReservation(UserInfo userInfo, String accessToken, String orgIpaCode, PivotSILPrenotaExportFlussoRiconciliazione request) {
    PivotSILPrenotaExportFlussoRiconciliazioneRisposta response = new PivotSILPrenotaExportFlussoRiconciliazioneRisposta();

    Pair<Long, OffsetDateTime> reservationResponse = doReservation(userInfo, accessToken, orgIpaCode,
      request.getIdUnivocoDovuto(),
      Optional.ofNullable(request.getDataUltimoAggiornamentoDa()).map(d -> d.toGregorianCalendar().toZonedDateTime().toOffsetDateTime()).orElse(null),
      Optional.ofNullable(request.getDataUltimoAggiornamentoA()).map(d -> d.toGregorianCalendar().toZonedDateTime().toOffsetDateTime()).orElse(null),
      () -> classificationsExportFileRequestMapper.mapToExportFileRequest(null, request));

    XMLGregorianCalendar lastUpdateXMLGregorianFrom = request.getDataUltimoAggiornamentoDa();
    XMLGregorianCalendar lastUpdateXMLGregorianTo = request.getDataUltimoAggiornamentoA();

    response.setDataA(lastUpdateXMLGregorianFrom != null && lastUpdateXMLGregorianTo != null ? lastUpdateXMLGregorianTo : null);
    response.setRequestToken(reservationResponse.getLeft().toString());

    return response;
  }

  public Pair<Long, OffsetDateTime> doReservation(UserInfo userInfo, String accessToken, String orgIpaCode,
                              String iud, OffsetDateTime lastUpdateDateFrom, OffsetDateTime lastUpdateDateTo,
                              Supplier<ClassificationsExportFileRequestDTO> requestDTOSupplier) {
    AuthorizationService.validateAdminRole(orgIpaCode, userInfo);
    Long organizationId = getOrganizationIdFromUserInfo(userInfo, orgIpaCode);
    getAndValidateDebtPositionTypeOrg(
      organizationId,
      iud,
      accessToken,
      SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO,
      SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO
    );

    ClassificationsExportFileRequestDTO requestDTO = requestDTOSupplier.get();
    requestDTO.organizationId(organizationId);

    Long exportFileId = exportFileService.createClassificationsExportFile(requestDTO, accessToken);

    log.debug("Export file created with ID: {}", exportFileId);

    return Pair.of(exportFileId, lastUpdateDateFrom != null && lastUpdateDateTo != null ? lastUpdateDateTo : null);
  }
}
