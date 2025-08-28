package it.gov.pagopa.pu.sil.dto;

import it.gov.pagopa.pu.sil.dto.generated.ManageDebtPositionDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class ManageDebtPositionWithIudDTO extends ManageDebtPositionDTO {
  private String iud;

  public static ManageDebtPositionWithIudDTO fromManageDebtPositionDTO(ManageDebtPositionDTO dto, String iud) {
    return ManageDebtPositionWithIudDTO.builder()
      .debtPositionDescription(dto.getDebtPositionDescription())
      .validityDate(dto.getValidityDate())
      .paymentOptionDescription(dto.getPaymentOptionDescription())
      .installments(dto.getInstallments())
      .iud(iud)
      .build();
  }
}
