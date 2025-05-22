package it.gov.pagopa.pu.sil.event.producer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistryEventDTO {
  private String eventId;
  private String traceId;
  private String eventOrigin;
  private OffsetDateTime eventDateTime;
  private String eventType;
  private String eventSubType;
  private String brokerFiscalCode;
  private String orgFiscalCode;
  private String nav;
  private String requestorId;
  private String grantorId;
  private String outcome;
  private String body;
}
