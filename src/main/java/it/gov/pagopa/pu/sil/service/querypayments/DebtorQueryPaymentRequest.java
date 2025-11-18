package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;

import java.time.OffsetDateTime;

public record DebtorQueryPaymentRequest(String ipaCode,
                                        PersonEntityType debtorEntityType,
                                        String debtorFiscalCode,
                                        OffsetDateTime dateFrom,
                                        OffsetDateTime dateTo) {}
