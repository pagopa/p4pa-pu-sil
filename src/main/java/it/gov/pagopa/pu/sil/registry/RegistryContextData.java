package it.gov.pagopa.pu.sil.registry;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RegistryContextData {
  private String orgFiscalCode;
  private RegistryEventType eventType;
  private UserInfo loggedUser;
  private String orgSilServiceName;
  private String iuv;
}
