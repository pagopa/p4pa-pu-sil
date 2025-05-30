package it.gov.pagopa.pu.sil.registry;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.enums.RegistryEventSubType;
import it.gov.pagopa.pu.sil.enums.RegistrySilEventType;
import it.gov.pagopa.pu.sil.enums.SilOutcome;
import it.gov.pagopa.pu.sil.event.producer.RegistryProducerService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistryLoggerTest {

  @Mock
  private JAXBTransformService jaxbTransformService;

  @Mock
  private RegistryProducerService registryProducerService;

  @InjectMocks
  private RegistryLogger registryLogger;

  private UserInfo mockUserInfo;

  @BeforeEach
  void setUp() {
    mockUserInfo = new UserInfo();
    mockUserInfo.setUserId("mockUserId");
  }

  @Test
  void testProduceReqRegistryEvent() {
    // Given
    String orgFiscalCode = "mockFiscalCode";
    RegistrySilEventType eventType = RegistrySilEventType.paaSILImportaDovuto;
    String iuv = "mockIUV";
    String blIuv = "businessLogicIUV";
    String xmlRequest = "<xml>mockRequest</xml>";
    Object request = new Object();
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn(xmlRequest);

    // When
    Object actualResponse = registryLogger.execute(orgFiscalCode, eventType, iuv, request, mockUserInfo, "mockOrgSilServiceName",
      () -> Triple.of(null, blIuv, SilOutcome.OK), e -> null);

    // Then
    assertNull(actualResponse);

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.REQ)),
      eq("mockUserId"),
      eq(RegistryProducerService.PU_ID),
      eq(iuv),
      any(),
      eq(SilOutcome.OK),
      eq(xmlRequest));

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.RESP)),
      eq(RegistryProducerService.PU_ID),
      eq("mockUserId"),
      eq(blIuv),
      any(),
      eq(SilOutcome.OK),
      any());
  }

  @Test
  void testProduceReqRegistryEventWithExtraInfo() {
    // Given
    String orgFiscalCode = "mockFiscalCode";
    RegistrySilEventType eventType = RegistrySilEventType.paaSILImportaDovuto;
    String iuv = "mockIUV";
    String xmlRequest = "<xml>mockRequest</xml>";
    Object request = new Object();
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn(xmlRequest);

    // When
    Object actualResponse = registryLogger.execute(orgFiscalCode, eventType, iuv, request, mockUserInfo, "mockOrgSilServiceName",
      () -> Triple.of(null, null, SilOutcome.OK), e -> null, () -> {
        // Simulate extra info retrieval
        return Map.of("extraInfoKey", "extraInfoValue");
      }, null);

    // Then
    assertNull(actualResponse);

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.REQ)),
      eq("mockUserId"),
      eq(RegistryProducerService.PU_ID),
      eq(iuv),
      any(),
      eq(SilOutcome.OK),
      argThat(o -> (o instanceof Map<?,?> m) &&
        m.containsKey("extraInfoKey") && "extraInfoValue".equals(m.get("extraInfoKey")) &&
        m.containsKey(RegistryLogger.XML_BODY_KEY) && xmlRequest.equals(m.get(RegistryLogger.XML_BODY_KEY)))
    );

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.RESP)),
      eq(RegistryProducerService.PU_ID),
      eq("mockUserId"),
      eq(iuv),
      any(),
      eq(SilOutcome.OK),
      any()
    );
  }

  @Test
  void testProduceReqRegistryEventWithExtraInfoSkipXmlBody() {
    // Given
    String orgFiscalCode = "mockFiscalCode";
    RegistrySilEventType eventType = RegistrySilEventType.paaSILImportaDovuto;
    String iuv = "mockIUV";
    Object request = new Object();

    // When
    Object actualResponse = registryLogger.execute(orgFiscalCode, eventType, iuv, request, mockUserInfo, "mockOrgSilServiceName",
      () -> Triple.of(null, null, SilOutcome.OK), e -> null, () -> {
        // Simulate extra info retrieval
        return Map.of("extraInfoKey", "extraInfoValue", RegistryLogger.SKIP_XML_BODY_KEY, true);
      }, null);

    // Then
    assertNull(actualResponse);

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.REQ)),
      eq("mockUserId"),
      eq(RegistryProducerService.PU_ID),
      eq(iuv),
      any(),
      eq(SilOutcome.OK),
      argThat(o -> (o instanceof Map<?,?> m) &&
        m.containsKey("extraInfoKey") && "extraInfoValue".equals(m.get("extraInfoKey")) &&
        !m.containsKey(RegistryLogger.XML_BODY_KEY) )
    );

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.RESP)),
      eq(RegistryProducerService.PU_ID),
      eq("mockUserId"),
      eq(iuv),
      any(),
      eq(SilOutcome.OK),
      any()
    );
  }

  @Test
  void testProduceRespRegistryEvent() {
    // Given
    String orgFiscalCode = "mockFiscalCode";
    RegistrySilEventType eventType = RegistrySilEventType.paaSILImportaDovuto;
    String iuv = "mockIUV";
    String blIuv = "businessLogicIUV";
    String xmlResponse = "<xml>mockResponse</xml>";
    Object request = new Object();
    Object response = new Object();
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn("<xml>mockRequest</xml>");
    when(jaxbTransformService.marshalling(eq(response), any())).thenReturn(xmlResponse);

    // When
    Object actualResponse = registryLogger.execute(orgFiscalCode, eventType, iuv, request, mockUserInfo, "mockOrgSilServiceName",
      () -> Triple.of(response, blIuv, SilOutcome.OK), e -> null);

    // Then
    assertEquals(response, actualResponse);

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.REQ)),
      eq("mockUserId"),
      eq(RegistryProducerService.PU_ID),
      eq(iuv),
      any(),
      eq(SilOutcome.OK),
      any()
    );

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.RESP)),
      eq(RegistryProducerService.PU_ID),
      eq("mockUserId"),
      eq(blIuv),
      any(),
      eq(SilOutcome.OK),
      eq(xmlResponse)
    );
  }

  @Test
  void testProduceRespRegistryEventWithExtraInfo() {
    // Given
    String orgFiscalCode = "mockFiscalCode";
    RegistrySilEventType eventType = RegistrySilEventType.paaSILImportaDovuto;
    String iuv = "mockIUV";
    String xmlResponse = "<xml>mockResponse</xml>";
    Object request = new Object();
    Object response = new Object();
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn("<xml>mockRequest</xml>");
    when(jaxbTransformService.marshalling(eq(response), any())).thenReturn(xmlResponse);

    // When
    Object actualResponse = registryLogger.execute(orgFiscalCode, eventType, iuv, request, mockUserInfo, "mockOrgSilServiceName",
      () -> Triple.of(response, null, SilOutcome.OK), e -> null, null, r -> {
        // Simulate extra info retrieval
        return Map.of("extraInfoKey", "extraInfoValue");
      });

    // Then
    assertEquals(response, actualResponse);

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.REQ)),
      eq("mockUserId"),
      eq(RegistryProducerService.PU_ID),
      eq(iuv),
      any(),
      eq(SilOutcome.OK),
      any()
    );

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      eq(RegistryEventSubType.RESP),
      eq(RegistryProducerService.PU_ID),
      eq("mockUserId"),
      eq(iuv),
      any(),
      eq(SilOutcome.OK),
      argThat(o -> (o instanceof Map<?,?> m) &&
        m.containsKey("extraInfoKey") && "extraInfoValue".equals(m.get("extraInfoKey")) &&
        m.containsKey(RegistryLogger.XML_BODY_KEY) && xmlResponse.equals(m.get(RegistryLogger.XML_BODY_KEY)))
    );
  }

  @Test
  void testProduceRespRegistryEventWithExtraInfoSkipXmlBody() {
    // Given
    String orgFiscalCode = "mockFiscalCode";
    RegistrySilEventType eventType = RegistrySilEventType.paaSILImportaDovuto;
    String iuv = "mockIUV";
    Object request = new Object();
    Object response = "responseString";
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn("<xml>mockRequest</xml>");

    // When
    Object actualResponse = registryLogger.execute(orgFiscalCode, eventType, iuv, request, mockUserInfo, "mockOrgSilServiceName",
      () -> Triple.of(response, null, SilOutcome.OK), e -> null, null, r -> {
        // Simulate extra info retrieval
        return Map.of("extraInfoKey", "extraInfoValue:"+r, RegistryLogger.SKIP_XML_BODY_KEY, true);
      });

    // Then
    assertEquals(response, actualResponse);

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.REQ)),
      eq("mockUserId"),
      eq(RegistryProducerService.PU_ID),
      eq(iuv),
      any(),
      eq(SilOutcome.OK),
      any()
    );

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.RESP)),
      eq(RegistryProducerService.PU_ID),
      eq("mockUserId"),
      eq(iuv),
      any(),
      eq(SilOutcome.OK),
      argThat(o -> (o instanceof Map<?,?> m) &&
        m.containsKey("extraInfoKey") && ("extraInfoValue:"+response).equals(m.get("extraInfoKey")) &&
        !m.containsKey(RegistryLogger.XML_BODY_KEY))
    );
  }

  @Test
  void testExecuteWithException() {
    // Given
    String orgFiscalCode = "mockFiscalCode";
    RegistrySilEventType eventType = RegistrySilEventType.paaSILImportaDovuto;
    String iuv = "mockIUV";
    String xmlRequest = "<xml>mockRequest</xml>";
    Object request = new Object();
    Object fallbackResponse = new Object();
    String xmlFallbackResponse = "mockFallbackResponse";
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn(xmlRequest);
    when(jaxbTransformService.marshalling(eq(fallbackResponse), any())).thenReturn(xmlFallbackResponse);

    // When
    Object actualResponse = registryLogger.execute(orgFiscalCode, eventType, iuv, request, mockUserInfo, "mockOrgSilServiceName",
      () -> {
        throw new RuntimeException("Mock Exception");
      }, e -> fallbackResponse);

    // Then
    assertEquals(fallbackResponse, actualResponse);

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.REQ)),
      eq("mockUserId"),
      eq(RegistryProducerService.PU_ID),
      eq(iuv),
      any(),
      eq(SilOutcome.OK),
      eq(xmlRequest));

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.RESP)),
      eq(RegistryProducerService.PU_ID),
      eq("mockUserId"),
      eq(iuv),
      any(),
      eq(SilOutcome.KO),
      argThat(xmlFallbackResponse::equals));
  }

  @Test
  void testNotifySilEventException() {
    // Given
    String orgFiscalCode = "mockFiscalCode";
    RegistrySilEventType eventType = RegistrySilEventType.paaSILImportaDovuto;
    String iuv = "mockIUV";
    String xmlRequest = "<xml>mockRequest</xml>";
    Object request = new Object();
    Object response = new Object();
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn(xmlRequest);
    doThrow(new RuntimeException("simulated exception")).when(registryProducerService).notifySilEvent(
      any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

    // When
    Object actualResponse = registryLogger.execute(orgFiscalCode, eventType, iuv, request, mockUserInfo, "mockOrgSilServiceName",
      () -> Triple.of(response, null, SilOutcome.OK), e -> null);

    // Then
    assertEquals(response, actualResponse);

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.REQ)),
      eq("mockUserId"),
      eq(RegistryProducerService.PU_ID),
      eq(iuv),
      any(),
      eq(SilOutcome.OK),
      eq(xmlRequest));

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.RESP)),
      eq(RegistryProducerService.PU_ID),
      eq("mockUserId"),
      eq(iuv),
      any(),
      eq(SilOutcome.OK),
      any());
  }

  @Test
  void testProduceReqRegistryClientEvent() {
    // Given
    String orgFiscalCode = "mockFiscalCode";
    RegistrySilEventType eventType = RegistrySilEventType.attualizzazioneImporti;
    String iuv = "mockIUV";
    String orgSilServiceName = "mockOrgSilServiceName";
    String xmlRequest = "<xml>mockRequest</xml>";
    Object request = new Object();
    when(jaxbTransformService.marshalling(eq(request), any())).thenReturn(xmlRequest);

    // When
    Object actualResponse = registryLogger.execute(orgFiscalCode, eventType, iuv, request, mockUserInfo, orgSilServiceName,
      () -> Triple.of(null, null, SilOutcome.OK), e -> null);

    // Then
    assertNull(actualResponse);

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.REQ)),
      eq(RegistryProducerService.PU_ID),
      eq(orgSilServiceName),
      eq(iuv),
      any(),
      eq(SilOutcome.OK),
      eq(xmlRequest));

    verify(registryProducerService).notifySilEvent(
      eq(mockUserInfo),
      eq(orgFiscalCode),
      eq(eventType),
      argThat(e -> e.equals(RegistryEventSubType.RESP)),
      eq(orgSilServiceName),
      eq(RegistryProducerService.PU_ID),
      eq(iuv),
      any(),
      eq(SilOutcome.OK),
      any());
  }
}
