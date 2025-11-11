package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.LocalDateIntervalFilter;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.veneto.regione.pagamenti.pivot.ente.IdUnivocoRendicontazioneType;
import it.veneto.regione.pagamenti.pivot.ente.IdUnivocoVersamentoType;
import it.veneto.regione.pagamenti.pivot.ente.PivotSILPrenotaExportFlussoRiconciliazione;
import it.veneto.regione.pagamenti.pivot.ente.TipoDovutoType;
import org.springframework.stereotype.Service;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
        .debtPositionTypeOrgCodes(Optional.ofNullable(request.getTipoDovuto())
          .map(TipoDovutoType::getTipos).map(HashSet::new).orElse(null))
        .label(request.getCodiceClassificazione().getClassificaziones().stream()
          .map(ClassificationsExportFileFilter.LabelEnum::fromValue)
          .collect(Collectors.toSet()))
        .iuv(Optional.ofNullable(request.getIdUnivocoVersamento())
          .map(IdUnivocoVersamentoType::getIuvs).orElse(null))
        .iur(Optional.ofNullable(request.getIdUnivocoRiscossione())
          .map(List::of).orElse(null))
        .iufs(Optional.ofNullable(request.getIdUnivocoRendicontazione())
          .map(IdUnivocoRendicontazioneType::getIurs).orElse(null))
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
    if (from == null && to == null) {
      return null;
    }

    return LocalDateIntervalFilter.builder()
      .from(ConversionUtils.toLocalDate(from))
      .to(ConversionUtils.toLocalDate(to))
      .build();
  }
}
