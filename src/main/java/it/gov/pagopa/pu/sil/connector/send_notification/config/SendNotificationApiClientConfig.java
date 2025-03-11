package it.gov.pagopa.pu.sil.connector.send_notification.config;

import it.gov.pagopa.pu.sil.config.ApiClientConfig;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rest.send-notification")
@SuperBuilder
@NoArgsConstructor
public class SendNotificationApiClientConfig extends ApiClientConfig {
}
