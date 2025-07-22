package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileFilter;
import it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileRequestDTO;
import it.gov.pagopa.pu.processexecutions.dto.generated.LocalDateIntervalFilter;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class MassiveExportRequestMapper {

  public static final String FILE_VERSION = "fileVersion";
  public static final String FROM = "from";
  public static final String TO = "to";
  public static final String DEBT_POSITION_TYPE_ORG_CODE = "debtPositionTypeOrgCode";
  public static final String INCREMENTAL = "incremental";

  public static final String IUD = "iud";
  public static final String LAST_UPDATE_DATE_FROM = "lastUpdateDateFrom";
  public static final String LAST_UPDATE_DATE_TO = "lastUpdateDateTo";

  public static final String VALUE_DATE_FROM = "valueDateFrom";
  public static final String VALUE_DATE_TO = "valueDateTo";
  public static final String BILL_DATE_FROM = "billDateFrom";
  public static final String BILL_DATE_TO = "billDateTo";
  public static final String REGULATION_DATE_FROM = "regulationDateFrom";
  public static final String REGULATION_DATE_TO = "regulationDateTo";
  public static final String PAY_DATE_FROM = "payDateFrom";
  public static final String PAY_DATE_TO = "payDateTo";
  public static final String PAYMENT_DATE_FROM = "paymentDateFrom";
  public static final String PAYMENT_DATE_TO = "paymentDateTo";
  public static final String PSP_LAST_NAME = "pspLastName";
  public static final String PSP_COMPANY_NAME = "pspCompanyName";
  public static final String REGULATION_UNIQUE_IDENTIFIER = "regulationUniqueIdentifier";
  public static final String ACCOUNT_REGISTRY_CODE = "accountRegistryCode";
  public static final String BILL_AMOUNT = "billAmount";
  public static final String REMITTANCE_INFORMATION = "remittanceInformation";
  public static final String SEPARATOR = ",";
  public static final String DEBT_POSITION_TYPE_ORG_CODES = "debtPositionTypeOrgCodes";
  public static final String LABEL = "label";
  public static final String IUV = "iuv";
  public static final String IUR = "iur";

  private final Optional<Map<String, String>> optExportFilters;
  private final Optional<Map<String, Boolean>> optExportFlags;

  public MassiveExportRequestMapper(Map<String, String> exportFilters,
                                    Map<String, Boolean> exportFlags) {
    this.optExportFilters = Optional.ofNullable(exportFilters);
    this.optExportFlags = Optional.ofNullable(exportFlags);
  }

  public Optional<String> getFilterFieldValue(String fieldName) {
    return optExportFilters.flatMap(f ->
      Optional.ofNullable(f.get(fieldName)));
  }

  public Optional<Boolean> getFlagFieldValue(String fieldName) {
    return optExportFlags.flatMap(f ->
      Optional.ofNullable(f.get(fieldName)));
  }

  public Optional<OffsetDateTime> parseFilterDate(String fieldName) {
    return getFilterFieldValue(fieldName)
    .flatMap(dateStr -> {
      try {
        return Optional.of(OffsetDateTime.parse(dateStr));
      } catch (Exception e) {
        log.error("Error parsing '{}' date: {}", fieldName, dateStr, e);
        return Optional.empty();
      }
    });
  }

  private LocalDateIntervalFilter localDateIntervalFilter(String fromFieldName, String toFieldName) {
    return LocalDateIntervalFilter.builder()
      .from(parseFilterDate(fromFieldName).map(OffsetDateTime::toLocalDate).orElse(null))
      .to(parseFilterDate(toFieldName).map(OffsetDateTime::toLocalDate).orElse(null))
      .build();
  }

  public ClassificationsExportFileRequestDTO mapToClassificationsExportFileRequest(String iud,
                                                                    OffsetDateTime lastUpdateDateFrom,
                                                                    OffsetDateTime lastUpdateDateTo) {
    return new ClassificationsExportFileRequestDTO()
      .exportFileType(ClassificationsExportFileRequestDTO.ExportFileTypeEnum.CLASSIFICATIONS)
      .fileVersion(getFilterFieldValue(FILE_VERSION).orElse(null))
      .filterFields(new ClassificationsExportFileFilter()
        .debtPositionTypeOrgCodes(getFilterFieldValue(DEBT_POSITION_TYPE_ORG_CODES)
          .map(s -> Set.of(s.split(SEPARATOR))).orElse(null))
        .label(getFilterFieldValue(LABEL)
          .map(s -> Arrays.stream(s.split(SEPARATOR))
            .map(ClassificationsExportFileFilter.LabelEnum::fromValue)
            .collect(Collectors.toSet())).orElse(null))
        .iuv(getFilterFieldValue(IUV)
          .map(s -> List.of(s.split(SEPARATOR))).orElse(null))
        .iur(getFilterFieldValue(IUR)
          .map(s -> List.of(s.split(SEPARATOR))).orElse(null))
        .iud(iud)
        .regionValueDate(localDateIntervalFilter(VALUE_DATE_FROM, VALUE_DATE_TO))
        .billDate(localDateIntervalFilter(BILL_DATE_FROM, BILL_DATE_TO))
        .regulationDate(localDateIntervalFilter(REGULATION_DATE_FROM, REGULATION_DATE_TO))
        .payDate(localDateIntervalFilter(PAY_DATE_FROM, PAY_DATE_TO))
        .paymentDate(localDateIntervalFilter(PAYMENT_DATE_FROM, PAYMENT_DATE_TO))
        .lastClassificationDate(LocalDateIntervalFilter.builder()
          .from(Optional.ofNullable(lastUpdateDateFrom).map(OffsetDateTime::toLocalDate).orElse(null))
          .to(Optional.ofNullable(lastUpdateDateTo).map(OffsetDateTime::toLocalDate).orElse(null))
          .build())
        .pspLastName(getFilterFieldValue(PSP_LAST_NAME).orElse(null))
        .pspCompanyName(getFilterFieldValue(PSP_COMPANY_NAME).orElse(null))
        .regulationUniqueIdentifier(getFilterFieldValue(REGULATION_UNIQUE_IDENTIFIER).orElse(null))
        .accountRegistryCode(getFilterFieldValue(ACCOUNT_REGISTRY_CODE).orElse(null))
        .billAmountCents(ConversionUtils.stringEuroAmountToCentsAmount(getFilterFieldValue(BILL_AMOUNT).orElse(null)))
        .remittanceInformation(getFilterFieldValue(REMITTANCE_INFORMATION).orElse(null))
      );
  }

}
