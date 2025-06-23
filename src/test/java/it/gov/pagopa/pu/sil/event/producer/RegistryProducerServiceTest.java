package it.gov.pagopa.pu.sil.event.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.registries.dto.generated.RegistryEventSubType;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.UtilitiesTest;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.event.producer.dto.RegistryEventDTO;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILAutorizzaImportFlusso;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import uk.co.jemos.podam.api.PodamFactory;

import java.time.OffsetDateTime;

@ExtendWith(MockitoExtension.class)
class RegistryProducerServiceTest {

  @Mock
  private StreamBridge streamBridge;

  private RegistryProducerService registryProducerService;

  private final PodamFactory podamFactory;

  private final ObjectMapper objectMapper;

  public RegistryProducerServiceTest() {
    podamFactory = TestUtils.getPodamFactory();
    this.objectMapper = new ObjectMapper();
    this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
  }

  @BeforeEach
  void setUp() {
    registryProducerService = new RegistryProducerService(streamBridge, new ObjectMapper());
  }

  @AfterEach
  void clear(){
    UtilitiesTest.clearTraceIdContext();
  }

  @ParameterizedTest
  @ValueSource(strings = {"null", "string", "object"})
  void whenNotifySilEventThenSendMessage(String bodyType) throws JsonProcessingException {
    // Given
    UserInfo loggedUser = podamFactory.manufacturePojo(UserInfo.class);
    String orgFiscalCode = loggedUser.getOrganizations().getFirst().getOrganizationFiscalCode();
    RegistryEventType eventType = podamFactory.manufacturePojo(RegistryEventType.class);
    RegistryEventSubType subType = podamFactory.manufacturePojo(RegistryEventSubType.class);
    String requestorId = "requestorId";
    String grantorId = "grantorId";
    String iuv = "iuv";
    String nav = "3iuv";
    RegistryOutcome outcome;
    Object body;
    String serializedBody = switch (bodyType) {
      case "null" -> {
        body = null;
        outcome = RegistryOutcome.OK;
        yield null;
      }
      case "string" -> {
        body = "string body";
        outcome = RegistryOutcome.KO;
        yield (String) body;
      }
      case "object" -> {
        body = podamFactory.manufacturePojo(PaaSILAutorizzaImportFlusso.class);
        outcome = podamFactory.manufacturePojo(RegistryOutcome.class);
        yield objectMapper.writeValueAsString(body);
      }
      default ->
        throw new IllegalArgumentException("Invalid body type: " + bodyType);
    };
    String traceId = "TRACEID";
    UtilitiesTest.setTraceId(traceId);

    RegistryContextData registryContextData = RegistryContextData.builder()
      .orgFiscalCode(orgFiscalCode)
      .loggedUser(loggedUser)
      .eventType(eventType)
      .iuv(iuv)
      .build();

    // When
    registryProducerService.notifySilEvent(registryContextData, subType, requestorId, grantorId, outcome, body);

    // Then
    Mockito.verify(streamBridge, Mockito.times(1)).send(
      Mockito.eq("registryProducer-out-0"),
      Mockito.any(),
      Mockito.<Message<?>>argThat(m -> {
        Assertions.assertEquals(String.valueOf(loggedUser.getOrganizations().getFirst().getOrganizationId()), m.getHeaders().get(KafkaHeaders.KEY));

        RegistryEventDTO payload = (RegistryEventDTO) m.getPayload();
        String eventIdPrefix = eventType.name();
        Assertions.assertEquals(eventIdPrefix, payload.getRegistryId().substring(0, eventIdPrefix.length()));
        Assertions.assertEquals(traceId, payload.getTraceId());
        Assertions.assertEquals("pu-sil", payload.getRegistryOrigin());
        Assertions.assertEquals("REGISTRY_SIL", payload.getRegistryType());

        Assertions.assertEquals(eventType, payload.getEventType());
        Assertions.assertEquals(subType, payload.getEventSubType());
        Assertions.assertEquals(loggedUser.getBrokerFiscalCode(), payload.getBrokerFiscalCode());
        Assertions.assertEquals(loggedUser.getOrganizations().getFirst().getOrganizationFiscalCode(), payload.getOrgFiscalCode());
        Assertions.assertEquals(iuv, payload.getIuv());
        Assertions.assertEquals(nav, payload.getNav());
        Assertions.assertEquals(requestorId, payload.getRequestorId());
        Assertions.assertEquals(grantorId, payload.getGrantorId());
        Assertions.assertEquals(outcome, payload.getOutcome());
        Assertions.assertEquals(serializedBody, payload.getBody());
        Assertions.assertTrue(OffsetDateTime.now().toEpochSecond() - payload.getDateTime().toEpochSecond() < 5);

        String[] ignoredFields = {};
        if(body == null) {
          ignoredFields = new String[]{"body"};
        }
        TestUtils.checkNotNullFields(payload, ignoredFields);

        return true;
      }));
  }
}
