package it.gov.pagopa.pu.sil.connector.actualization.client;

import it.gov.pagopa.actualization.dto.generated.Payment;
import it.gov.pagopa.actualization.dto.generated.UpdatedPayment;
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
public class ActualizationClient {
  private final ActualizationApisHolder actualizationApisHolder;
  private final RegistryLogger registryLogger;

  public ActualizationClient(ActualizationApisHolder actualizationApisHolder,
                             RegistryLogger registryLogger) {
    this.actualizationApisHolder = actualizationApisHolder;
    this.registryLogger = registryLogger;
  }

  public UpdatedPayment actualization(OrgSilServiceDTO orgSilServiceDTO, UserInfo loggedUser, String accessToken, Payment request) {
    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(request.getOrgFiscalCode())
      .eventType(RegistryEventType.SIL_attualizzazioneImporti)
      .orgSilServiceName(orgSilServiceDTO.getApplicationName())
      .iuv(Utilities.nav2Iuv(request.getNav()))
      .loggedUser(loggedUser)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> {
        String url = orgSilServiceDTO.getServiceUrl().replace("/amount-update", "");
        return Triple.of(
          actualizationApisHolder.getActualizationNativeApi(accessToken, url).actualization(request),
          null,
          RegistryOutcome.OK
        );
      },
      null
    );
  }
}
