package it.gov.pagopa.pu.sil.connector.paymentnotification.client;

import it.gov.pagopa.paymentnotification.legacy.dto.generated.PaymentNotification;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.paymentnotification.config.PaymentNotificationApisHolder;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.util.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LegacyPaymentNotificationClient {
  private final PaymentNotificationApisHolder paymentNotificationApisHolder;
  private final RegistryLogger registryLogger;

  public LegacyPaymentNotificationClient(PaymentNotificationApisHolder paymentNotificationApisHolder,
                                         RegistryLogger registryLogger) {
    this.paymentNotificationApisHolder = paymentNotificationApisHolder;
    this.registryLogger = registryLogger;
  }

  public void notifyPayment(String orgFiscalCode, OrgSilServiceDTO orgSilServiceDTO, String nav, UserInfo loggedUser, String accessToken, PaymentNotification paymentNotification) {
    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .eventType(RegistryEventType.SIL_notificaPagamento)
      .orgSilServiceName(orgSilServiceDTO.getApplicationName())
      .iuv(Utilities.nav2Iuv(nav))
      .loggedUser(loggedUser)
      .build();

    registryLogger.execute(
      contextData,
      paymentNotification,
      () -> {
        paymentNotificationApisHolder.getPaymentNotificationLegacyApi(accessToken, orgSilServiceDTO.getServiceUrl().replace("/payment-notification", ""))
          .paymentNotification(paymentNotification);
        return Triple.of(Void.TYPE,
          null,
          RegistryOutcome.OK
        );
      },
      null
    );
  }
}
