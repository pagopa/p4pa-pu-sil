package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.actualization.config.ActualizationApisHolder;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LegacyBasicAuthClient {
  private final ActualizationApisHolder actualizationApisHolder;
  private final RegistryLogger registryLogger;

  public LegacyBasicAuthClient(ActualizationApisHolder actualizationApisHolder,
                               RegistryLogger registryLogger) {
    this.actualizationApisHolder = actualizationApisHolder;
    this.registryLogger = registryLogger;
  }

  public Token login(RegistryContextData contextData, Credentials credentials, String authUrl) {
    return registryLogger.execute(
      contextData,
      credentials,
      () -> Triple.of(
        actualizationApisHolder.getAmountUpdatesLegacyApi(null, authUrl)
          .login(credentials),
        null,
        RegistryOutcome.OK
      ),
      null
    );
  }
}
