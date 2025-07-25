package it.gov.pagopa.pu.sil.config.agid;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rest.integrity-data")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PuIntegrityDataConfig {
  private String audience;
  private String clientId;
  private String privateKey;
  private String publicKey;
  private Long expirationMinutes;
}
