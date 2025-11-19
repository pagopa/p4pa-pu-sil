package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;

import java.time.OffsetDateTime;

public record DebtorQueryPaymentRequest(String ipaCode,
                                        PersonEntityType debtorEntityType,
                                        String debtorFiscalCode,
                                        InstallmentStatus status,
                                        OffsetDateTime dateFrom,
                                        OffsetDateTime dateTo) {

  public OffsetDateTimeIntervalFilter getDateFilter() {
    return new OffsetDateTimeIntervalFilter(dateFrom, dateTo);
  }
}
