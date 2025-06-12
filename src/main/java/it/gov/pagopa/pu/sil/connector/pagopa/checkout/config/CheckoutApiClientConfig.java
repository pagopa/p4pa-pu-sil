package it.gov.pagopa.pu.sil.connector.pagopa.checkout.config;

import it.gov.pagopa.pu.sil.config.rest.ApiClientConfig;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rest.pagopa-node-services.checkout")
@SuperBuilder
@NoArgsConstructor
public class CheckoutApiClientConfig extends ApiClientConfig {
}
