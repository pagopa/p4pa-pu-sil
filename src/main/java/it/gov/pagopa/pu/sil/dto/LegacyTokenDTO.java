package it.gov.pagopa.pu.sil.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegacyTokenDTO {
  private String token;
  private Integer expiresIn;
}
