package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.connector.actualization.client.LegacyActualizationClient;
import org.springframework.stereotype.Component;

@Component
public class LegacyActualizationServiceImpl implements LegacyActualizationService {
  private final LegacyActualizationClient legacyActualizationClient;

  public LegacyActualizationServiceImpl(LegacyActualizationClient legacyActualizationClient) {
    this.legacyActualizationClient = legacyActualizationClient;
  }

  @Override
  public Token login(Credentials credentials, String authUrl) {
    return legacyActualizationClient.login(credentials, authUrl);
  }

  @Override
  public PagamentoAggiornato actualization(String accessToken, String serviceUrl, Pagamento pagamento) {
    return legacyActualizationClient.actualization(accessToken, serviceUrl, pagamento);
  }
}
