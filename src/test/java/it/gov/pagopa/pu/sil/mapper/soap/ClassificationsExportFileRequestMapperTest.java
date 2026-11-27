package it.gov.pagopa.pu.sil.mapper.soap;

import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.pivot.ente.*;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.Month;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationsExportFileRequestMapperTest {
  private final ClassificationsExportFileRequestMapper mapper = new ClassificationsExportFileRequestMapper();

  @Test
  void whenMapToExportFileRequestThenMapAllInputFields() throws Exception {
    PivotSILPrenotaExportFlussoRiconciliazione request = new PivotSILPrenotaExportFlussoRiconciliazione();
    request.setVersioneTracciato("v1");
    request.setIdUnivocoDovuto("IUD123");
    request.setOrdinante("PSP Last Name");
    request.setDenominazioneAttestante("PSP Company");
    request.setIdRegolamento("REG123");
    request.setContoTesoreria("ACC123");
    request.setImportoTesoreria("100.50");
    request.setCausale("Remittance Info");

    XMLGregorianCalendar from = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar(2023, Calendar.JANUARY, 1));
    XMLGregorianCalendar to = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar(2023, Calendar.DECEMBER, 31));
    request.setDataValutaDa(from);
    request.setDataValutaA(to);
    request.setDataContabileDa(from);
    request.setDataContabileA(to);
    request.setDataRegolamentoDa(from);
    request.setDataRegolamentoA(to);
    request.setDataEsecuzioneDa(from);
    request.setDataEsecuzioneA(to);
    request.setDataEsitoDa(from);
    request.setDataEsitoA(to);
    TipoDovutoType tipoDovutoType = new TipoDovutoType();
    tipoDovutoType.getTipos().add("TIPO_DOVUTO");
    request.setTipoDovuto(tipoDovutoType);
    CodiceClassificazioneType codiceClassificazioneType = new CodiceClassificazioneType();
    codiceClassificazioneType.getClassificaziones().add("RT_IUF");
    request.setCodiceClassificazione(codiceClassificazioneType);
    IdUnivocoVersamentoType idUnivocoVersamentoType = new IdUnivocoVersamentoType();
    idUnivocoVersamentoType.getIuvs().add("IUV123");
    request.setIdUnivocoVersamento(idUnivocoVersamentoType);
    IdUnivocoRendicontazioneType idUnivocoRendicontazioneType = new IdUnivocoRendicontazioneType();
    idUnivocoRendicontazioneType.getIurs().add("IUF123");
    request.setIdUnivocoRendicontazione(idUnivocoRendicontazioneType);
    request.setIdUnivocoRiscossione("IUR123");

    Long orgId = 123L;
    ClassificationsExportFileRequestDTO dto = mapper.mapToExportFileRequest(orgId, request);
    assertNotNull(dto);
    TestUtils.checkNotNullFields(dto);
    assertEquals(orgId, dto.getOrganizationId());
    assertEquals(ClassificationsExportFileRequestDTO.ExportFileTypeEnum.CLASSIFICATIONS, dto.getExportFileType());
    assertEquals("v1", dto.getFileVersion());
    ClassificationsExportFileFilter filter = dto.getFilterFields();
    assertNotNull(filter);
    TestUtils.checkNotNullFields(filter, "lastClassificationDate");
    assertEquals("IUD123", filter.getIud());
    assertEquals(LocalDate.of(2023, Month.JANUARY, 1), Objects.requireNonNull(filter.getRegionValueDate()).getFrom());
    assertEquals(LocalDate.of(2023, Month.DECEMBER, 31), filter.getRegionValueDate().getTo());
    assertEquals("PSP Last Name", filter.getPspLastName());
    assertEquals("PSP Company", filter.getPspCompanyName());
    assertEquals("REG123", filter.getRegulationUniqueIdentifier());
    assertEquals("ACC123", filter.getAccountRegistryCode());
    assertEquals(Long.valueOf(10050), filter.getBillAmountCents());
    assertEquals("Remittance Info", filter.getRemittanceInformation());
    assertTrue(Objects.requireNonNull(filter.getLabel()).contains(ClassificationsExportFileFilter.LabelEnum.RT_IUF));
  }

  @Test
  void givenEmptyClassificazionesFieldWhenMapToExportFileRequestThenFilterShouldContainAllLabels() throws Exception {
    PivotSILPrenotaExportFlussoRiconciliazione request = new PivotSILPrenotaExportFlussoRiconciliazione();
    request.setVersioneTracciato("v1");
    request.setIdUnivocoDovuto("IUD123");
    request.setOrdinante("PSP Last Name");
    request.setDenominazioneAttestante("PSP Company");
    request.setIdRegolamento("REG123");
    request.setContoTesoreria("ACC123");
    request.setImportoTesoreria("100.50");
    request.setCausale("Remittance Info");

    XMLGregorianCalendar from = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar(2023, Calendar.JANUARY, 1));
    XMLGregorianCalendar to = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar(2023, Calendar.DECEMBER, 31));
    request.setDataValutaDa(from);
    request.setDataValutaA(to);
    request.setDataContabileDa(from);
    request.setDataContabileA(to);
    request.setDataRegolamentoDa(from);
    request.setDataRegolamentoA(to);
    request.setDataEsecuzioneDa(from);
    request.setDataEsecuzioneA(to);
    request.setDataEsitoDa(from);
    request.setDataEsitoA(to);
    TipoDovutoType tipoDovutoType = new TipoDovutoType();
    tipoDovutoType.getTipos().add("TIPO_DOVUTO");
    request.setTipoDovuto(tipoDovutoType);
    CodiceClassificazioneType codiceClassificazioneType = new CodiceClassificazioneType();
    request.setCodiceClassificazione(codiceClassificazioneType);
    IdUnivocoVersamentoType idUnivocoVersamentoType = new IdUnivocoVersamentoType();
    idUnivocoVersamentoType.getIuvs().add("IUV123");
    request.setIdUnivocoVersamento(idUnivocoVersamentoType);
    IdUnivocoRendicontazioneType idUnivocoRendicontazioneType = new IdUnivocoRendicontazioneType();
    idUnivocoRendicontazioneType.getIurs().add("IUF123");
    request.setIdUnivocoRendicontazione(idUnivocoRendicontazioneType);
    request.setIdUnivocoRiscossione("IUR123");

    Long orgId = 123L;
    ClassificationsExportFileRequestDTO dto = mapper.mapToExportFileRequest(orgId, request);
    assertNotNull(dto);
    TestUtils.checkNotNullFields(dto);
    assertEquals(orgId, dto.getOrganizationId());
    assertEquals(ClassificationsExportFileRequestDTO.ExportFileTypeEnum.CLASSIFICATIONS, dto.getExportFileType());
    assertEquals("v1", dto.getFileVersion());
    ClassificationsExportFileFilter filter = dto.getFilterFields();
    assertNotNull(filter);
    TestUtils.checkNotNullFields(filter, "lastClassificationDate");
    assertEquals("IUD123", filter.getIud());
    assertEquals(LocalDate.of(2023, Month.JANUARY, 1), Objects.requireNonNull(filter.getRegionValueDate()).getFrom());
    assertEquals(LocalDate.of(2023, Month.DECEMBER, 31), filter.getRegionValueDate().getTo());
    assertEquals("PSP Last Name", filter.getPspLastName());
    assertEquals("PSP Company", filter.getPspCompanyName());
    assertEquals("REG123", filter.getRegulationUniqueIdentifier());
    assertEquals("ACC123", filter.getAccountRegistryCode());
    assertEquals(Long.valueOf(10050), filter.getBillAmountCents());
    assertEquals("Remittance Info", filter.getRemittanceInformation());
    assertEquals(EnumSet.allOf(ClassificationsExportFileFilter.LabelEnum.class), filter.getLabel());
  }
}
