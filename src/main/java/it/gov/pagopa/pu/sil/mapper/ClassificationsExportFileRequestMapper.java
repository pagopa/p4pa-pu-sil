package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.LocalDateIntervalFilter;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILPrenotaExportFlussoRiconciliazione;
import org.springframework.stereotype.Service;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClassificationsExportFileRequestMapper {
  public ClassificationsExportFileRequestDTO mapToExportFileRequest(Long organizationId,
                                                                    PivotSILPrenotaExportFlussoRiconciliazione request) {
    return new ClassificationsExportFileRequestDTO()
      .organizationId(organizationId)
      .exportFileType(ClassificationsExportFileRequestDTO.ExportFileTypeEnum.CLASSIFICATIONS)
      .fileVersion(request.getVersioneTracciato())
      .filterFields(new ClassificationsExportFileFilter()
        .debtPositionTypeOrgCodes(Set.of(request.getTipoDovuto().getTipos().toArray(new String[0])))
        .label(request.getCodiceClassificazione().getClassificaziones().stream()
          .map(ClassificationsExportFileFilter.LabelEnum::fromValue)
          .collect(Collectors.toSet()))
        .iuv(request.getIdUnivocoVersamento().getIuvs())
        .iur(request.getIdUnivocoRendicontazione().getIurs())
        .iud(request.getIdUnivocoDovuto())
        .regionValueDate(mapToLocalDateIntervalFilter(request.getDataValutaDa(), request.getDataValutaA()))
        .billDate(mapToLocalDateIntervalFilter(request.getDataContabileDa(), request.getDataContabileA()))
        .regulationDate(mapToLocalDateIntervalFilter(request.getDataRegolamentoDa(), request.getDataRegolamentoA()))
        .payDate(mapToLocalDateIntervalFilter(request.getDataEsecuzioneDa(), request.getDataEsecuzioneA()))
        .paymentDate(mapToLocalDateIntervalFilter(request.getDataEsitoDa(), request.getDataEsitoA()))
        .lastClassificationDate(mapToLocalDateIntervalFilter(request.getDataUltimoAggiornamentoDa(), request.getDataUltimoAggiornamentoA()))
        .pspLastName(request.getOrdinante())
        .pspCompanyName(request.getDenominazioneAttestante())
        .regulationUniqueIdentifier(request.getIdRegolamento())
        .accountRegistryCode(request.getContoTesoreria())
        .billAmountCents(ConversionUtils.stringEuroAmountToCentsAmount(request.getImportoTesoreria()))
        .remittanceInformation(request.getCausale())
      );
  }

  private LocalDateIntervalFilter mapToLocalDateIntervalFilter(XMLGregorianCalendar from, XMLGregorianCalendar to) {
    Function<XMLGregorianCalendar, LocalDate> toLocalDate = xmlGregorianCalendar ->
      xmlGregorianCalendar.toGregorianCalendar().toZonedDateTime().toLocalDate();
    return LocalDateIntervalFilter.builder()
      .from(toLocalDate.apply(from))
      .to(toLocalDate.apply(to))
      .build();
  }
}
