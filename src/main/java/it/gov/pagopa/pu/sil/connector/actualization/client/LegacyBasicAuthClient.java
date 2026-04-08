package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.actualization.config.LegacyActualizationApisHolder;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.util.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LegacyBasicAuthClient {
  private final LegacyActualizationApisHolder legacyActualizationApisHolder;
  private final RegistryLogger registryLogger;
  private final String auxDigit;

  public LegacyBasicAuthClient(LegacyActualizationApisHolder legacyActualizationApisHolder,
                               RegistryLogger registryLogger,
                               @Value("${nav.aux-digit}") String auxDigit) {
    this.legacyActualizationApisHolder = legacyActualizationApisHolder;
    this.registryLogger = registryLogger;
    this.auxDigit = auxDigit;
  }

  public Token login(String orgFiscalCode, String orgSilServiceName, String nav, UserInfo loggedUser, Credentials credentials, String authUrl) {
    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.SIL_attualizzazioneImporti)
      .orgSilServiceName(orgSilServiceName)
      .iuv(Utilities.nav2Iuv(nav, auxDigit))
      .loggedUser(loggedUser)
      .build();

    return registryLogger.execute(
      contextData,
      credentials,
      () -> Triple.of(
        legacyActualizationApisHolder.getAmountUpdatesLegacyApi(null, authUrl.replace("/login", ""))
          .login(credentials),
        null,
        RegistryOutcome.OK
      ),
      null
    );
  }
}
