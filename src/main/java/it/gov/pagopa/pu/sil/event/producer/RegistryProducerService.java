package it.gov.pagopa.pu.sil.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.auth.dto.generated.UserOrganizationRoles;
import it.gov.pagopa.pu.sil.enums.RegistryEventSubType;
import it.gov.pagopa.pu.sil.enums.RegistrySilEventType;
import it.gov.pagopa.pu.sil.enums.SilOutcome;
import it.gov.pagopa.pu.sil.event.producer.dto.RegistryEventDTO;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.util.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Component
public class RegistryProducerService {

  public static final String PU_ID = "piattaforma-unitaria";

  @Value("${spring.cloud.stream.bindings.registryProducer-out-0.binder}")
  private String binder;

  private final StreamBridge streamBridge;

  private final ObjectMapper objectMapper;

  public RegistryProducerService(StreamBridge streamBridge, ObjectMapper objectMapper) {
    this.streamBridge = streamBridge;
    this.objectMapper = objectMapper;
  }

  @Configuration
  static class RegistryProducerConfig {
    @Bean
    public Supplier<Message<RegistryEventDTO>> registryProducer() {
      return () -> null;
    }
  }

  public void notifySilEvent(UserInfo loggedUser, String orgFiscalCode,
                             RegistrySilEventType eventType, RegistryEventSubType subType,
                             String requestorId, String grantorId,
                             String iuv, String nav, SilOutcome outcome, Object body) {
    UserOrganizationRoles userOrg = loggedUser.getOrganizations().stream()
      .filter(x -> StringUtils.equals(x.getOrganizationFiscalCode(), orgFiscalCode))
      .findFirst()
      .orElseThrow(() -> new ApplicationException("invalid user for organization " + orgFiscalCode));

    String registryId = String.join("-", eventType.name(), String.valueOf(System.currentTimeMillis()), UUID.randomUUID().toString());
    String traceId = Utilities.getTraceId();

    String bodyString = null;
    if (body instanceof String bodyAsString) {
      bodyString = bodyAsString;
    } else if (body != null) {
      bodyString = serializeObjectToJson(body);
    }

    streamBridge.send("registryProducer-out-0", binder,
      MessageBuilder.withPayload(RegistryEventDTO.builder()
          .registryId(registryId)
          .traceId(traceId)
          .registryOrigin("pu-sil")
          .registryType("REGISTRY_SIL")
          .dateTime(OffsetDateTime.now())
          .eventType(eventType.name())
          .eventSubType(subType.name())
          .brokerFiscalCode(loggedUser.getBrokerFiscalCode())
          .orgFiscalCode(userOrg.getOrganizationFiscalCode())
          .iuv(iuv)
          .nav(nav)
          .requestorId(requestorId)
          .grantorId(grantorId)
          .outcome(outcome.name())
          .body(bodyString)
          .build()
        )
        .setHeader(KafkaHeaders.KEY, String.valueOf(userOrg.getOrganizationId()))
        .build()
    );
  }

  private String serializeObjectToJson(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (Exception e) {
      log.error("Error serializing object to JSON", e);
      throw new ApplicationException("Error serializing object to JSON", e);
    }
  }
}
