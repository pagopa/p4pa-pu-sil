package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.actualization.client.LegacyActualizationClient;
import org.springframework.stereotype.Component;


@Component
public class LegacyActualizationServiceImpl implements LegacyActualizationService {
  private final LegacyActualizationClient legacyActualizationClient;


  public LegacyActualizationServiceImpl(LegacyActualizationClient legacyActualizationClient) {
    this.legacyActualizationClient = legacyActualizationClient;
  }

  @Override
  public PagamentoAggiornato actualization(String orgFiscalCode, OrgSilServiceDTO orgSilServiceDTO, UserInfo loggedUser, String accessToken, Pagamento pagamento) {
      return legacyActualizationClient.actualization(orgFiscalCode, orgSilServiceDTO, loggedUser, accessToken, pagamento);
  }


}
