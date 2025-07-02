package it.gov.pagopa.pu.sil.registry;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.registries.dto.generated.RegistryEventSubType;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.event.producer.RegistryProducerService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegistryLoggerTest {

  @Mock
  private JAXBTransformService jaxbTransformService;

  @Mock
  private RegistryProducerService registryProducerService;

  @InjectMocks
  private RegistryLogger registryLogger;

  private UserInfo mockUserInfo;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    mockUserInfo = new UserInfo();
    mockUserInfo.setUserId("mockUserId");
  }

  @Test
  void testProduceReqRegistryEvent() {
    // Given
    RegistryContextData contextData = podamFactory.manufacturePojo(RegistryContextData.class);
    contextData.setEventType(RegistryEventType.paaSILImportaDovuto);
    contextData.setLoggedUser(mockUserInfo);
    contextData.setOrgSilServiceName("mockOrgSilServiceName");

    String blIuv = "businessLogicIUV";
    String xmlRequest = "<xml>mockRequest</xml>";
    Object request = new Object();
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn(xmlRequest);

    // When
    Object actualResponse = registryLogger.execute(contextData, request,
      () -> Triple.of(null, blIuv, RegistryOutcome.OK), e -> null);

    // Then
    assertNull(actualResponse);

    verify(registryProducerService).notifySilEvent(
      contextData,
      RegistryEventSubType.REQ,
      "mockUserId",
      RegistryLogger.PU_ID,
      RegistryOutcome.OK,
      xmlRequest);

    contextData.setIuv(blIuv);
    verify(registryProducerService).notifySilEvent(
      eq(contextData),
      eq(RegistryEventSubType.RESP),
      eq(RegistryLogger.PU_ID),
      eq("mockUserId"),
      eq(RegistryOutcome.OK),
      isNull());
  }

  @Test
  void testProduceReqRegistryEventWithExtraInfo() {
    // Given
    RegistryContextData contextData = podamFactory.manufacturePojo(RegistryContextData.class);
    contextData.setEventType(RegistryEventType.paaSILImportaDovuto);
    contextData.setLoggedUser(mockUserInfo);
    contextData.setOrgSilServiceName("mockOrgSilServiceName");

    String xmlRequest = "<xml>mockRequest</xml>";
    Object request = new Object();
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn(xmlRequest);

    // When
    Object actualResponse = registryLogger.execute(contextData, request,
      () -> Triple.of(null, null, RegistryOutcome.OK), e -> null, () -> {
        // Simulate extra info retrieval
        return Map.of("extraInfoKey", "extraInfoValue");
      }, null);

    // Then
    assertNull(actualResponse);

    verify(registryProducerService).notifySilEvent(
      eq(contextData),
      eq(RegistryEventSubType.REQ),
      eq("mockUserId"),
      eq(RegistryLogger.PU_ID),
      eq(RegistryOutcome.OK),
      argThat(o -> (o instanceof Map<?,?> m) &&
        m.containsKey("extraInfoKey") && "extraInfoValue".equals(m.get("extraInfoKey")) &&
        m.containsKey(RegistryLogger.XML_BODY_KEY) && xmlRequest.equals(m.get(RegistryLogger.XML_BODY_KEY)))
    );

    verify(registryProducerService).notifySilEvent(
      eq(contextData),
      eq(RegistryEventSubType.RESP),
      eq(RegistryLogger.PU_ID),
      eq("mockUserId"),
      eq(RegistryOutcome.OK),
      isNull()
    );
  }

  @Test
  void testProduceReqRegistryEventWithExtraInfoSkipXmlBody() {
    // Given
    RegistryContextData contextData = podamFactory.manufacturePojo(RegistryContextData.class);
    contextData.setEventType(RegistryEventType.paaSILImportaDovuto);
    contextData.setLoggedUser(mockUserInfo);
    contextData.setOrgSilServiceName("mockOrgSilServiceName");

    Object request = new Object();

    // When
    Object actualResponse = registryLogger.execute(contextData, request,
      () -> Triple.of(null, null, RegistryOutcome.OK), e -> null, () -> {
        // Simulate extra info retrieval
        return Map.of("extraInfoKey", "extraInfoValue", RegistryLogger.SKIP_XML_BODY_KEY, true);
      }, null);

    // Then
    assertNull(actualResponse);

    verify(registryProducerService).notifySilEvent(
      eq(contextData),
      eq(RegistryEventSubType.REQ),
      eq("mockUserId"),
      eq(RegistryLogger.PU_ID),
      eq(RegistryOutcome.OK),
      argThat(o -> (o instanceof Map<?,?> m) &&
        m.containsKey("extraInfoKey") && "extraInfoValue".equals(m.get("extraInfoKey")) &&
        !m.containsKey(RegistryLogger.XML_BODY_KEY) )
    );

    verify(registryProducerService).notifySilEvent(
      eq(contextData),
      eq(RegistryEventSubType.RESP),
      eq(RegistryLogger.PU_ID),
      eq("mockUserId"),
      eq(RegistryOutcome.OK),
      isNull()
    );
  }

  @Test
  void testProduceRespRegistryEvent() {
    // Given
    RegistryContextData contextData = podamFactory.manufacturePojo(RegistryContextData.class);
    contextData.setEventType(RegistryEventType.paaSILImportaDovuto);
    contextData.setLoggedUser(mockUserInfo);
    contextData.setOrgSilServiceName("mockOrgSilServiceName");

    String blIuv = "businessLogicIUV";
    String xmlRequest = "<xml>mockRequest</xml>";
    String xmlResponse = "<xml>mockResponse</xml>";
    Object request = new Object();
    Object response = new Object();
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn(xmlRequest);
    when(jaxbTransformService.marshalling(eq(response), any())).thenReturn(xmlResponse);

    // When
    Object actualResponse = registryLogger.execute(contextData, request,
      () -> Triple.of(response, blIuv, RegistryOutcome.OK), e -> null);

    // Then
    assertEquals(response, actualResponse);

    verify(registryProducerService).notifySilEvent(
      contextData,
      RegistryEventSubType.REQ,
      "mockUserId",
      RegistryLogger.PU_ID,
      RegistryOutcome.OK,
      xmlRequest
    );

    contextData.setIuv(blIuv);
    verify(registryProducerService).notifySilEvent(
      contextData,
      RegistryEventSubType.RESP,
      RegistryLogger.PU_ID,
      "mockUserId",
      RegistryOutcome.OK,
      xmlResponse
    );
  }

  @Test
  void testProduceRespRegistryEventWithExtraInfo() {
    // Given
    RegistryContextData contextData = podamFactory.manufacturePojo(RegistryContextData.class);
    contextData.setEventType(RegistryEventType.paaSILImportaDovuto);
    contextData.setLoggedUser(mockUserInfo);
    contextData.setOrgSilServiceName("mockOrgSilServiceName");

    String xmlRequest = "<xml>mockRequest</xml>";
    String xmlResponse = "<xml>mockResponse</xml>";
    Object request = new Object();
    Object response = new Object();
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn(xmlRequest);
    when(jaxbTransformService.marshalling(eq(response), any())).thenReturn(xmlResponse);

    // When
    Object actualResponse = registryLogger.execute(contextData, request,
      () -> Triple.of(response, null, RegistryOutcome.OK), e -> null, null, r -> {
        // Simulate extra info retrieval
        return Map.of("extraInfoKey", "extraInfoValue");
      });

    // Then
    assertEquals(response, actualResponse);

    verify(registryProducerService).notifySilEvent(
      contextData,
      RegistryEventSubType.REQ,
      "mockUserId",
      RegistryLogger.PU_ID,
      RegistryOutcome.OK,
      xmlRequest
    );

    verify(registryProducerService).notifySilEvent(
      eq(contextData),
      eq(RegistryEventSubType.RESP),
      eq(RegistryLogger.PU_ID),
      eq("mockUserId"),
      eq(RegistryOutcome.OK),
      argThat(o -> (o instanceof Map<?,?> m) &&
        m.containsKey("extraInfoKey") && "extraInfoValue".equals(m.get("extraInfoKey")) &&
        m.containsKey(RegistryLogger.XML_BODY_KEY) && xmlResponse.equals(m.get(RegistryLogger.XML_BODY_KEY)))
    );
  }

  @Test
  void testProduceRespRegistryEventWithExtraInfoSkipXmlBody() {
    // Given
    RegistryContextData contextData = podamFactory.manufacturePojo(RegistryContextData.class);
    contextData.setEventType(RegistryEventType.paaSILImportaDovuto);
    contextData.setLoggedUser(mockUserInfo);
    contextData.setOrgSilServiceName("mockOrgSilServiceName");

    Object request = new Object();
    Object response = "responseString";
    String xmlRequest = "<xml>mockRequest</xml>";
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn(xmlRequest);

    // When
    Object actualResponse = registryLogger.execute(contextData, request,
      () -> Triple.of(response, null, RegistryOutcome.OK), e -> null, null, r -> {
        // Simulate extra info retrieval
        return Map.of("extraInfoKey", "extraInfoValue:"+r, RegistryLogger.SKIP_XML_BODY_KEY, true);
      });

    // Then
    assertEquals(response, actualResponse);

    verify(registryProducerService).notifySilEvent(
      contextData,
      RegistryEventSubType.REQ,
      "mockUserId",
      RegistryLogger.PU_ID,
      RegistryOutcome.OK,
      xmlRequest
    );

    verify(registryProducerService).notifySilEvent(
      eq(contextData),
      eq(RegistryEventSubType.RESP),
      eq(RegistryLogger.PU_ID),
      eq("mockUserId"),
      eq(RegistryOutcome.OK),
      argThat(o -> (o instanceof Map<?,?> m) &&
        m.containsKey("extraInfoKey") && ("extraInfoValue:"+response).equals(m.get("extraInfoKey")) &&
        !m.containsKey(RegistryLogger.XML_BODY_KEY))
    );
  }

  @Test
  void testExecuteWithException() {
    // Given
    RegistryContextData contextData = podamFactory.manufacturePojo(RegistryContextData.class);
    contextData.setEventType(RegistryEventType.paaSILImportaDovuto);
    contextData.setLoggedUser(mockUserInfo);
    contextData.setOrgSilServiceName("mockOrgSilServiceName");

    String xmlRequest = "<xml>mockRequest</xml>";
    Object request = new Object();
    Object fallbackResponse = new Object();
    String xmlFallbackResponse = "mockFallbackResponse";
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn(xmlRequest);
    when(jaxbTransformService.marshalling(eq(fallbackResponse), any())).thenReturn(xmlFallbackResponse);

    // When
    Object actualResponse = registryLogger.execute(contextData, request,
      () -> {
        throw new RuntimeException("Mock Exception");
      }, e -> fallbackResponse);

    // Then
    assertEquals(fallbackResponse, actualResponse);

    verify(registryProducerService).notifySilEvent(
      contextData,
      RegistryEventSubType.REQ,
      "mockUserId",
      RegistryLogger.PU_ID,
      RegistryOutcome.OK,
      xmlRequest);

    verify(registryProducerService).notifySilEvent(
      contextData,
      RegistryEventSubType.RESP,
      RegistryLogger.PU_ID,
      "mockUserId",
      RegistryOutcome.KO,
      xmlFallbackResponse);
  }

  @Test
  void testNotifySilEventException() {
    // Given
    RegistryContextData contextData = podamFactory.manufacturePojo(RegistryContextData.class);
    contextData.setEventType(RegistryEventType.paaSILImportaDovuto);
    contextData.setLoggedUser(mockUserInfo);
    contextData.setOrgSilServiceName("mockOrgSilServiceName");

    String xmlRequest = "<xml>mockRequest</xml>";
    Object request = new Object();
    Object response = new Object();
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn(xmlRequest);
    doThrow(new RuntimeException("simulated exception")).when(registryProducerService).notifySilEvent(
      any(), any(), any(), any(), any(), any());

    // When
    Object actualResponse = registryLogger.execute(contextData, request,
      () -> Triple.of(response, null, RegistryOutcome.OK), e -> null);

    // Then
    assertEquals(response, actualResponse);

    verify(registryProducerService).notifySilEvent(
      contextData,
      RegistryEventSubType.REQ,
      "mockUserId",
      RegistryLogger.PU_ID,
      RegistryOutcome.OK,
      xmlRequest);

    verify(registryProducerService).notifySilEvent(
      eq(contextData),
      eq(RegistryEventSubType.RESP),
      eq(RegistryLogger.PU_ID),
      eq("mockUserId"),
      eq(RegistryOutcome.OK),
      isNull());
  }

  @Test
  void testProduceReqRegistryClientEvent() {
    // Given
    RegistryContextData contextData = podamFactory.manufacturePojo(RegistryContextData.class);
    contextData.setEventType(RegistryEventType.attualizzazioneImporti);
    contextData.setLoggedUser(mockUserInfo);
    contextData.setOrgSilServiceName("mockOrgSilServiceName");

    String xmlRequest = "<xml>mockRequest</xml>";
    Object request = new Object();
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn(xmlRequest);

    // When
    Object actualResponse = registryLogger.execute(contextData, request,
      () -> Triple.of(null, null, RegistryOutcome.OK), e -> null);

    // Then
    assertNull(actualResponse);

    verify(registryProducerService).notifySilEvent(
      contextData,
      RegistryEventSubType.REQ,
      RegistryLogger.PU_ID,
      "mockOrgSilServiceName",
      RegistryOutcome.OK,
      xmlRequest);

    verify(registryProducerService).notifySilEvent(
      eq(contextData),
      eq(RegistryEventSubType.RESP),
      eq(contextData.getOrgSilServiceName()),
      eq(RegistryLogger.PU_ID),
      eq(RegistryOutcome.OK),
      isNull());
  }

  public static void configureRegistryLoggerMock(RegistryLogger registryLoggerMock, RegistryContextData contextData, Object request, boolean withExtraInfo) {
    Object[] result = new Object[1];
    Exception[] exception = new Exception[1];
    ArgumentMatcher<Supplier<Triple<Object, String, RegistryOutcome>>> requestHandler = i -> {
      try {
        result[0] = i.get().getLeft();
      } catch (Exception e) {
        exception[0] = e;
      }
      return true;
    };
    ArgumentMatcher<Function<Exception, Object>> exceptionHandler = i -> {
      if (exception[0] != null && i != null) {
        result[0] = i.apply(exception[0]);
        exception[0] = null;
      }
      return true;
    };

    Answer<Object> answer = i -> {
      if(exception[0] != null) {
        throw exception[0];
      } else {
        return result[0];
      }
    };

    if(withExtraInfo) {
      Mockito.when(registryLoggerMock.execute(
        Mockito.eq(contextData),
        Mockito.same(request),
        Mockito.argThat(requestHandler),
        Mockito.argThat(exceptionHandler),
        Mockito.argThat(requestExtraInfoRetriever -> {
          requestExtraInfoRetriever.get();
          return true;
        }),
        Mockito.argThat(responseExtraInfoExtractor  -> {
          if (result[0] != null) {
            responseExtraInfoExtractor.apply(result[0]);
          }
          return true;
        })
      )).thenAnswer(answer);
    } else {
      Mockito.when(registryLoggerMock.execute(
        Mockito.eq(contextData),
        Mockito.same(request),
        Mockito.argThat(requestHandler),
        Mockito.argThat(exceptionHandler)
      )).thenAnswer(answer);
    }
  }
}
