package it.gov.pagopa.pu.sil.dto;

import it.gov.pagopa.pu.sil.dto.generated.ManageDebtPositionDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ManageDebtPositionWithIudDTO extends ManageDebtPositionDTO {
  private String iud;

  public static ManageDebtPositionWithIudDTO fromManageDebtPositionDTO(ManageDebtPositionDTO dto, String iud) {
    ManageDebtPositionWithIudDTO enrichedDTO = new ManageDebtPositionWithIudDTO();

    enrichedDTO.setDebtPositionDescription(dto.getDebtPositionDescription());
    enrichedDTO.setValidityDate(dto.getValidityDate());
    enrichedDTO.setPaymentOptionDescription(dto.getPaymentOptionDescription());
    enrichedDTO.setInstallments(dto.getInstallments());
    enrichedDTO.setIud(iud);

    return enrichedDTO;
  }
}
