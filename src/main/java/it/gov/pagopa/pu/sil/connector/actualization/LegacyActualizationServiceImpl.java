package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.connector.actualization.client.LegacyActualizationClient;
import it.gov.pagopa.pu.sil.dto.generated.AmountUpdatesDTO;
import it.gov.pagopa.pu.sil.mapper.AmountUpdatesMapper;
import org.springframework.stereotype.Component;

@Component
public class LegacyActualizationServiceImpl implements LegacyActualizationService {
  private final LegacyActualizationClient legacyActualizationClient;
  private final AmountUpdatesMapper amountUpdatesMapper;

  public LegacyActualizationServiceImpl(LegacyActualizationClient legacyActualizationClient,
                                        AmountUpdatesMapper amountUpdatesMapper) {
    this.legacyActualizationClient = legacyActualizationClient;
    this.amountUpdatesMapper = amountUpdatesMapper;
  }

  @Override
  public Token login(Credentials credentials, String authUrl) {
    return legacyActualizationClient.login(credentials, authUrl);
  }

  @Override
  public AmountUpdatesDTO actualization(String accessToken, String serviceUrl, Pagamento pagamento) {
    PagamentoAggiornato actualization = legacyActualizationClient.actualization(accessToken, serviceUrl, pagamento);
    return amountUpdatesMapper.pagamentoAggiornato2AmountUpdatesDTO(actualization);
  }
}
