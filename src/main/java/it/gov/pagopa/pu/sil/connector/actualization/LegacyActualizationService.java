package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;

public interface LegacyActualizationService {
  Token login(Credentials credentials, String authUrl);
  PagamentoAggiornato actualization(String accessToken, String serviceUrl, Pagamento pagamento);
}
