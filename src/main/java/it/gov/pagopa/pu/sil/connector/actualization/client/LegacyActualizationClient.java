package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.actualization.config.ActualizationApisHolder;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.util.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LegacyActualizationClient {
  private final ActualizationApisHolder actualizationApisHolder;
  private final RegistryLogger registryLogger;

  public LegacyActualizationClient(ActualizationApisHolder actualizationApisHolder,
                                   RegistryLogger registryLogger) {
    this.actualizationApisHolder = actualizationApisHolder;
    this.registryLogger = registryLogger;
  }

  public PagamentoAggiornato actualization(String orgFiscalCode, OrgSilServiceDTO orgSilServiceDTO, UserInfo loggedUser, String accessToken, Pagamento pagamento) {
    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.SIL_attualizzazioneImporti)
      .orgSilServiceName(orgSilServiceDTO.getApplicationName())
      .iuv(Utilities.nav2Iuv(pagamento.getNumeroAvviso()))
      .loggedUser(loggedUser)
      .build();

    return registryLogger.execute(
      contextData,
      pagamento,
      () -> Triple.of(actualizationApisHolder.getAmountUpdatesLegacyApi(accessToken, orgSilServiceDTO.getServiceUrl().replace("/notification-price", ""))
          .attualizzazione(pagamento),
        null,
        RegistryOutcome.OK
      ),
      null
    );
  }
}
