package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MassiveExportRequestMapperTest {

  @Test
  void whenMapToClassificationsExportFileRequestThenMapAllFields() throws Exception {

    Map<String, String> exportFilters = new HashMap<>();

    exportFilters.put(MassiveExportRequestMapper.FILE_VERSION, "v1");
    exportFilters.put(MassiveExportRequestMapper.IUD, "IUD123");
    exportFilters.put(MassiveExportRequestMapper.PSP_LAST_NAME, "PSP Last Name");
    exportFilters.put(MassiveExportRequestMapper.PSP_COMPANY_NAME, "PSP Company");
    exportFilters.put(MassiveExportRequestMapper.REGULATION_UNIQUE_IDENTIFIER, "REG123");
    exportFilters.put(MassiveExportRequestMapper.ACCOUNT_REGISTRY_CODE, "ACC123");
    exportFilters.put(MassiveExportRequestMapper.BILL_AMOUNT, "100.50");
    exportFilters.put(MassiveExportRequestMapper.REMITTANCE_INFORMATION, "Remittance Info");

    OffsetDateTime fromODT = OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    String from = fromODT.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    OffsetDateTime toODT = OffsetDateTime.of(2023, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC);
    String to = toODT.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    exportFilters.put(MassiveExportRequestMapper.VALUE_DATE_FROM, from);
    exportFilters.put(MassiveExportRequestMapper.VALUE_DATE_TO, to);
    exportFilters.put(MassiveExportRequestMapper.BILL_DATE_FROM, from);
    exportFilters.put(MassiveExportRequestMapper.BILL_DATE_TO, to);
    exportFilters.put(MassiveExportRequestMapper.REGULATION_DATE_FROM, from);
    exportFilters.put(MassiveExportRequestMapper.REGULATION_DATE_TO, to);
    exportFilters.put(MassiveExportRequestMapper.PAY_DATE_FROM, from);
    exportFilters.put(MassiveExportRequestMapper.PAY_DATE_TO, to);
    exportFilters.put(MassiveExportRequestMapper.PAYMENT_DATE_FROM, from);
    exportFilters.put(MassiveExportRequestMapper.PAYMENT_DATE_TO, to);
    exportFilters.put(MassiveExportRequestMapper.LAST_UPDATE_DATE_FROM, from);
    exportFilters.put(MassiveExportRequestMapper.LAST_UPDATE_DATE_TO, to);
    exportFilters.put(MassiveExportRequestMapper.DEBT_POSITION_TYPE_ORG_CODES, "TIPO_DOVUTO_1,TIPO_DOVUTO_2");
    exportFilters.put(MassiveExportRequestMapper.LABEL, ClassificationsExportFileFilter.LabelEnum.DOPPI.getValue() + "," +
      ClassificationsExportFileFilter.LabelEnum.RT_IUF.getValue());
    exportFilters.put(MassiveExportRequestMapper.IUV, "IUV123,IUV456");
    exportFilters.put(MassiveExportRequestMapper.IUR, "IUR123,IUR456");

    MassiveExportRequestMapper mapper = new MassiveExportRequestMapper(exportFilters, new HashMap<>());

    String iud = "IUD123";
    ClassificationsExportFileRequestDTO dto = mapper.mapToClassificationsExportFileRequest(iud, fromODT, toODT);
    assertNotNull(dto);
    assertEquals(ClassificationsExportFileRequestDTO.ExportFileTypeEnum.CLASSIFICATIONS, dto.getExportFileType());
    assertEquals("v1", dto.getFileVersion());
    ClassificationsExportFileFilter filter = dto.getFilterFields();
    assertNotNull(filter);
    assertEquals("IUD123", filter.getIud());
    assertEquals(LocalDate.of(2023, 1, 1), filter.getRegionValueDate().getFrom());
    assertEquals(LocalDate.of(2023, 12, 31), filter.getRegionValueDate().getTo());
    assertEquals("PSP Last Name", filter.getPspLastName());
    assertEquals("PSP Company", filter.getPspCompanyName());
    assertEquals("REG123", filter.getRegulationUniqueIdentifier());
    assertEquals("ACC123", filter.getAccountRegistryCode());
    assertEquals(Long.valueOf(10050), filter.getBillAmountCents());
    assertEquals("Remittance Info", filter.getRemittanceInformation());
    assertTrue(filter.getLabel().contains(ClassificationsExportFileFilter.LabelEnum.RT_IUF));
  }
}
