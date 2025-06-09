package it.gov.pagopa.pu.sil.registry;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.enums.RegistryEventSubType;
import it.gov.pagopa.pu.sil.enums.RegistrySilEventType;
import it.gov.pagopa.pu.sil.enums.SilOutcome;
import it.gov.pagopa.pu.sil.event.producer.RegistryProducerService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.Utilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
@Slf4j
public class RegistryLogger {

  public static final String SKIP_XML_BODY_KEY = "skipXmlBody";
  public static final String XML_BODY_KEY = "xmlBody";
  public static final String IUV_SEPARATOR = ",";

  private final JAXBTransformService jaxbTransformService;

  private final RegistryProducerService registryProducerService;

  public RegistryLogger(JAXBTransformService jaxbTransformService, RegistryProducerService registryProducerService) {
    this.jaxbTransformService = jaxbTransformService;
    this.registryProducerService = registryProducerService;
  }

  public <I, O> O execute(String orgFiscalCode, RegistrySilEventType eventType, String iuv, I request, UserInfo loggedUser, String orgSilServiceName,
                          Supplier<Triple<O, String, SilOutcome>> requestHandler, Function<Exception, O> exceptionHandler) {
    return this.execute(orgFiscalCode, eventType, iuv, request, loggedUser, orgSilServiceName, requestHandler, exceptionHandler, null, null);
  }

  public <I, O> O execute(String orgFiscalCode, RegistrySilEventType eventType, String iuv, I request, UserInfo loggedUser, String orgSilServiceName,
                          Supplier<Triple<O, String, SilOutcome>> requestHandler, Function<Exception, O> exceptionHandler,
                          Supplier<Map<String, Object>> registryBodyRequestExtraInfoRetriever, Function<O, Map<String, Object>> registryBodyResponseExtraInfoExtractor) {
    produceReqRegistryEvent(orgFiscalCode, eventType, iuv, request, loggedUser, orgSilServiceName, registryBodyRequestExtraInfoRetriever);
    Triple<O, String, SilOutcome> response2outcome = Triple.of(null, null, SilOutcome.KO);
    try {
      response2outcome = requestHandler.get();
    } catch (Exception e) {
      response2outcome = Triple.of(exceptionHandler.apply(e), null, SilOutcome.KO);
    } finally {
      produceRespRegistryEvent(orgFiscalCode, eventType, StringUtils.firstNonBlank(response2outcome.getMiddle(), iuv),
        response2outcome.getLeft(), response2outcome.getRight(), loggedUser, orgSilServiceName, registryBodyResponseExtraInfoExtractor);
    }
    return response2outcome.getLeft();
  }

  private <I> void produceReqRegistryEvent(String orgFiscalCode, RegistrySilEventType eventType, String iuv, I request, UserInfo loggedUser,
                                           String orgSilServiceName, Supplier<Map<String, Object>> registryBodyRequestExtraInfoRetriever) {
    try {
      Object body = null;
      if (registryBodyRequestExtraInfoRetriever != null) {
        Map<String, Object> bodyMap = new HashMap<>(registryBodyRequestExtraInfoRetriever.get());
        if (bodyMap.containsKey(SKIP_XML_BODY_KEY)) {
          bodyMap.remove(SKIP_XML_BODY_KEY);
        } else if (request != null) {
          bodyMap.put(XML_BODY_KEY, jaxbTransformService.marshalling(request, (Class<I>) request.getClass()));
        }
        body = bodyMap;
      } else if (request != null) {
        body = jaxbTransformService.marshalling(request, (Class<I>) request.getClass());
      }
      produceRegistryEvent(orgFiscalCode, eventType, iuv, body, loggedUser, orgSilServiceName, RegistryEventSubType.REQ, SilOutcome.OK);
    } catch (Exception e) {
      log.error("Error producing request registry event for orgFiscalCode: {}, eventType: {}, iuv: {}", orgFiscalCode, eventType, iuv, e);
      // In case of error in producing the request event, we do not throw an exception to avoid breaking the flow
      // but we log the error and continue with the response event.
    }
  }

  private <O> void produceRespRegistryEvent(String orgFiscalCode, RegistrySilEventType eventType, String iuv, O response, SilOutcome outcome,
                                            UserInfo loggedUser, String orgSilServiceName, Function<O, Map<String, Object>> registryBodyResponseExtraInfoExtractor) {
    try {
      Object body = null;
      if (registryBodyResponseExtraInfoExtractor != null) {
        Map<String, Object> bodyMap = new HashMap<>(registryBodyResponseExtraInfoExtractor.apply(response));
        if (bodyMap.containsKey(SKIP_XML_BODY_KEY)) {
          bodyMap.remove(SKIP_XML_BODY_KEY);
        } else if (response != null) {
          bodyMap.put(XML_BODY_KEY, jaxbTransformService.marshalling(response, (Class<O>) response.getClass()));
        }
        body = bodyMap;
      } else if (response != null) {
        body = jaxbTransformService.marshalling(response, (Class<O>) response.getClass());
      }
      produceRegistryEvent(orgFiscalCode, eventType, iuv, body, loggedUser, orgSilServiceName, RegistryEventSubType.RESP, outcome);
    } catch (Exception e) {
      log.error("Error producing response registry event for orgFiscalCode: {}, eventType: {}, iuv: {}", orgFiscalCode, eventType, iuv, e);
      // In case of error in producing the response event, we do not throw an exception to avoid breaking the flow
      // but we log the error.
    }
  }

  private void produceRegistryEvent(String orgFiscalCode, RegistrySilEventType eventType, String iuv, Object body,
                                    UserInfo loggedUser, String orgSilServiceName, RegistryEventSubType eventSubType, SilOutcome outcome) {
    String requestorId;
    String grantorId;
    if (eventType.isExposedByPU() && RegistryEventSubType.REQ.equals(eventSubType)) {
      requestorId = loggedUser.getUserId();
      grantorId = RegistryProducerService.PU_ID;
    } else if (eventType.isExposedByPU() && RegistryEventSubType.RESP.equals(eventSubType)) {
      requestorId = RegistryProducerService.PU_ID;
      grantorId = loggedUser.getUserId();
    } else if (RegistryEventSubType.REQ.equals(eventSubType)) {
      requestorId = RegistryProducerService.PU_ID;
      grantorId = orgSilServiceName;
    } else {
      requestorId = orgSilServiceName;
      grantorId = RegistryProducerService.PU_ID;
    }
    registryProducerService.notifySilEvent(
      loggedUser,
      orgFiscalCode,
      eventType,
      eventSubType,
      requestorId,
      grantorId,
      iuv,
      Utilities.iuv2Nav(iuv),
      outcome,
      body);
  }
}
