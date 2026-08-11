package it.gov.pagopa.pu.sil.connector.sil.actualization;

import it.gov.pagopa.sil.actualizationlegacy.dto.generated.Pagamento;
import it.gov.pagopa.sil.actualizationlegacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;

public interface LegacyActualizationService {
  PagamentoAggiornato actualization(String orgFiscalCode, OrgSilServiceDTO orgSilService, UserInfo loggedUser, String accessToken, Pagamento pagamento);
}
