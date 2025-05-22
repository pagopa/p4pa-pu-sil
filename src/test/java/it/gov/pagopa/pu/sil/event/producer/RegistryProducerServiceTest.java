package it.gov.pagopa.pu.sil.event.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.UtilitiesTest;
import it.gov.pagopa.pu.sil.enums.RegistryEventSubType;
import it.gov.pagopa.pu.sil.enums.RegistrySilEventType;
import it.gov.pagopa.pu.sil.event.producer.dto.RegistryEventDTO;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILAutorizzaImportFlusso;
import org.apache.commons.lang3.RandomUtils;
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
    registryProducerService = new RegistryProducerService(streamBridge);
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
    RegistrySilEventType eventType = podamFactory.manufacturePojo(RegistrySilEventType.class);
    RegistryEventSubType subType = podamFactory.manufacturePojo(RegistryEventSubType.class);
    String requestorId = "requestorId";
    String grantorId = "grantorId";
    String iuv = "iuv";
    String nav = "nav";
    boolean outcomeOk;
    Object body;
    String serializedBody = switch (bodyType) {
      case "null" -> {
        body = null;
        outcomeOk = true;
        yield null;
      }
      case "string" -> {
        body = "string body";
        outcomeOk = false;
        yield (String) body;
      }
      case "object" -> {
        body = podamFactory.manufacturePojo(PaaSILAutorizzaImportFlusso.class);
        outcomeOk = RandomUtils.insecure().randomBoolean();
        yield objectMapper.writeValueAsString(body);
      }
      default ->
        throw new IllegalArgumentException("Invalid body type: " + bodyType);
    };
    String traceId = "TRACEID";
    UtilitiesTest.setTraceId(traceId);

    // When
    registryProducerService.notifySilEvent(loggedUser, orgFiscalCode, eventType, subType, requestorId, grantorId, iuv, nav, outcomeOk, body);

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

        Assertions.assertEquals(eventType.name(), payload.getEventType());
        Assertions.assertEquals(subType.name(), payload.getEventSubType());
        Assertions.assertEquals(loggedUser.getBrokerFiscalCode(), payload.getBrokerFiscalCode());
        Assertions.assertEquals(loggedUser.getOrganizations().getFirst().getOrganizationFiscalCode(), payload.getOrgFiscalCode());
        Assertions.assertEquals(iuv, payload.getIuv());
        Assertions.assertEquals(nav, payload.getNav());
        Assertions.assertEquals(requestorId, payload.getRequestorId());
        Assertions.assertEquals(grantorId, payload.getGrantorId());
        Assertions.assertEquals(outcomeOk ? "OK" : "KO", payload.getOutcome());
        Assertions.assertEquals(serializedBody, payload.getBody());
        Assertions.assertTrue(OffsetDateTime.now().toEpochSecond() - payload.getDateTime().toEpochSecond() < 5);
        return true;
      }));
  }
}
