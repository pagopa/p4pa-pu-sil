package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.sil.dto.generated.QueryPaymentStatusType;

public record PaymentStatusRequest(String organizationIpaCode, QueryPaymentStatusType idType, String id, boolean withReceiptBytes) {}

