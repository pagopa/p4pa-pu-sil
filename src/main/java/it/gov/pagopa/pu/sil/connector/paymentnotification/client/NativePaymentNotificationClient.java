package it.gov.pagopa.pu.sil.connector.paymentnotification.client;

import it.gov.pagopa.paymentnotification.dto.generated.PaymentNotificationRequest;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.connector.paymentnotification.config.PaymentNotificationApisHolder;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NativePaymentNotificationClient {
  private final PaymentNotificationApisHolder paymentNotificationApisHolder;
  private final RegistryLogger registryLogger;

  public NativePaymentNotificationClient(PaymentNotificationApisHolder paymentNotificationApisHolder,
                                         RegistryLogger registryLogger) {
    this.paymentNotificationApisHolder = paymentNotificationApisHolder;
    this.registryLogger = registryLogger;
  }

  public void notifyPayment(OrgSilServiceDTO orgSilServiceDTO, UserInfo loggedUser, String accessToken, PaymentNotificationRequest paymentNotificationRequest) {
    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(paymentNotificationRequest.getPaymentData().getOrgFiscalCode())
      .eventType(RegistryEventType.SIL_notificaPagamento)
      .orgSilServiceName(orgSilServiceDTO.getApplicationName())
      .iuv(paymentNotificationRequest.getPaymentData().getIuv())
      .loggedUser(loggedUser)
      .build();

    registryLogger.execute(
      contextData,
      paymentNotificationRequest,
      () -> {
        paymentNotificationApisHolder.getPaymentNotificationNativeApi(accessToken, orgSilServiceDTO.getServiceUrl().replace("/payment-notification", ""))
          .paymentNotification(paymentNotificationRequest);
        return Triple.of(Void.TYPE,
          null,
          RegistryOutcome.OK
        );
      },
      null
    );
  }
}
