package it.gov.pagopa.pu.sil.connector.debtpositions.config;

import it.gov.pagopa.pu.sil.config.rest.ApiClientConfig;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rest.debt-position")
@SuperBuilder
@NoArgsConstructor
public class DebtPositionsApiClientConfig extends ApiClientConfig {
}
