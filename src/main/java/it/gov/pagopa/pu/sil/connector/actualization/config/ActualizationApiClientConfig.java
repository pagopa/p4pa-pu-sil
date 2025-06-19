package it.gov.pagopa.pu.sil.connector.actualization.config;

import it.gov.pagopa.pu.sil.config.rest.ApiClientConfig;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rest.amount-updates")
@SuperBuilder
@NoArgsConstructor
public class ActualizationApiClientConfig extends ApiClientConfig {
}
