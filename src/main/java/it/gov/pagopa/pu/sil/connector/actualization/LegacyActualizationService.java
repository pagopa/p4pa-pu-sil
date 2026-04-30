package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;

public interface LegacyActualizationService {
  PagamentoAggiornato actualization(String orgFiscalCode, OrgSilServiceDTO orgSilService, UserInfo loggedUser, String accessToken, Pagamento pagamento);
}
