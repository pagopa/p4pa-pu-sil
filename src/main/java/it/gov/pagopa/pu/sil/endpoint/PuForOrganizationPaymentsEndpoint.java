package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.enums.RegistryEventSubType;
import it.gov.pagopa.pu.sil.enums.RegistrySilEventType;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.SilOutcome;
import it.gov.pagopa.pu.sil.event.producer.RegistryProducerService;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.util.Utilities;
import it.gov.pagopa.pu.sil.util.soap.FaultUtils;
import it.gov.pagopa.pu.sil.util.soap.SoapUtils;
import it.veneto.regione.pagamenti.ente.*;
import it.veneto.regione.pagamenti.ente.ppthead.IntestazionePPT;
import it.veneto.regione.schemas._2012.pagamenti.ente.Dovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.DovutiEntiSecondari;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.server.endpoint.annotation.SoapHeader;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Endpoint
@Slf4j
public class PuForOrganizationPaymentsEndpoint {
  public static final String NAMESPACE_URI = "http://www.regione.veneto.it/pagamenti/ente/";
  public static final String NAME = "PagamentiTelematiciDovutiPagati";

  private final RegistryProducerService registryProducerService;

  public PuForOrganizationPaymentsEndpoint(RegistryProducerService registryProducerService) {
    this.registryProducerService = registryProducerService;
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILAutorizzaImportFlusso")
  @ResponsePayload
  public PaaSILAutorizzaImportFlussoRisposta paaSILAutorizzaImportFlusso(
    @RequestPayload PaaSILAutorizzaImportFlusso request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    IntestazionePPT intestazionePPT = SoapUtils.unmarshallHeader(header, IntestazionePPT.class);
    log.info("processing paaSILAutorizzaImportFlusso codIpaEnte[{}]", Optional.ofNullable(intestazionePPT).map(IntestazionePPT::getCodIpaEnte).orElse(null));

    //check if the logged user has the right to call this endpoint
    if (intestazionePPT == null || !SecurityUtils.isAdminUser(intestazionePPT.getCodIpaEnte())) {
      log.error("User [{}] not authorized to call paaSILAutorizzaImportFlusso for organization {}",
        Optional.ofNullable(SecurityUtils.getLoggedUser()).map(UserInfo::getUserId).orElse(null),
        Optional.ofNullable(intestazionePPT).map(IntestazionePPT::getCodIpaEnte).orElse(null));
      return notAuthorizedFaultResponse(PaaSILAutorizzaImportFlussoRisposta::new);
    }

    //TODO: implement the logic to handle the SOAP Action P4ADEV-2892
    return FaultUtils.setFaultOnResponse(
      new PaaSILAutorizzaImportFlussoRisposta(),
      SilFaults.PAA_SYSTEM_ERROR,
      intestazionePPT.getCodIpaEnte(),
      FaultBean::new,
      PaaSILAutorizzaImportFlussoRisposta::setFault
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILImportaDovuto")
  @ResponsePayload
  public PaaSILImportaDovutoRisposta paaSILImportaDovuto(
    @RequestPayload PaaSILImportaDovuto request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {

    String orgIpaCode = getOrganizationIpaCodeFromHeader(header, "paaSILImportaDovuto");
    PaaSILImportaDovutoRisposta response = new PaaSILImportaDovutoRisposta();
    response.setEsito(SilOutcome.KO.name());

    //check if the logged user has the right to call this endpoint
    String clientId = Optional.ofNullable(SecurityUtils.getLoggedUser()).map(UserInfo::getUserId).orElse(null);
    if (!SecurityUtils.isAdminUser(orgIpaCode)) {
      log.error("ClientId [{}] not authorized to call paaSILImportaDovuto for organization {}", clientId, orgIpaCode);
      return notAuthorizedFaultResponse(() -> response);
    }

    String orgFiscalCode = SecurityUtils.getOrganizationInfoFromLoggedUser(orgIpaCode).getOrganizationFiscalCode();

    String eventIuv = null;
    Object eventBody = null;
    SilOutcome outcome = SilOutcome.KO;
    //try-catch-finally block to handle the response and create the RESPONSE event body
    try {

      //try-catch-finally block to handle the unmarshalling of the request and create the REQUEST event body
      try {
        //TODO P4ADEV-3013 : unmarshall the Dovuti object
        Dovuti dovutiObject = null;
        //TODO P4ADEV-3013 : unmarshall the DovutiEntiSecondari object
        DovutiEntiSecondari dovutiEntiSecondariObject = null;

        eventIuv = dovutiObject.getDatiVersamento().getIdentificativoUnivocoVersamento();
        Map<String, Object> eventBodyMap = new HashMap<>();
        eventBodyMap.put("flagGeneraIuv", request.isFlagGeneraIuv());
        eventBodyMap.put("dovuto", dovutiObject);
        if (dovutiEntiSecondariObject != null) {
          eventBodyMap.put("dovutoSecondario", dovutiEntiSecondariObject);
        }
        eventBody = eventBodyMap;
        outcome = SilOutcome.OK;
      } finally {
        registryProducerService.notifySilEvent(SecurityUtils.getLoggedUser(), orgFiscalCode,
          RegistrySilEventType.paaSILImportaDovuto, RegistryEventSubType.REQ,
          clientId, RegistryProducerService.PU_ID, eventIuv, Utilities.iuv2Nav(eventIuv),
          outcome, ObjectUtils.firstNonNull(eventBody, request));
        eventBody = null; // reset eventBody to avoid sending it for response event
      }

      //TODO P4ADEV-3015..3019: implement business logic
      eventIuv = response.getIdentificativoUnivocoVersamento();
      eventBody = String.format("debtPositionId[%s]","TODO"); //TODO P4ADEV-3015..3019: replace with actual debt position ID
      outcome = SilOutcome.OK;
    } catch (Exception e) {
      outcome = SilOutcome.KO;
      systemErrorFaultResponse(() -> response, e);
      eventBody = String.format("exception[%s] %s",e.getClass().getName(), e);
    } finally {
      response.setEsito(outcome.name());

      registryProducerService.notifySilEvent(SecurityUtils.getLoggedUser(), orgFiscalCode,
        RegistrySilEventType.paaSILImportaDovuto, RegistryEventSubType.RESP,
        clientId, RegistryProducerService.PU_ID, eventIuv, Utilities.iuv2Nav(eventIuv),
        outcome, eventBody);
    }

    return response;
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediAvvisiPendenti")
  @ResponsePayload
  public PaaSILChiediAvvisiPendentiRisposta paaSILChiediAvvisiPendenti(
    @RequestPayload PaaSILChiediAvvisiPendenti request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return FaultUtils.setFaultOnResponse(
      new PaaSILChiediAvvisiPendentiRisposta(),
      SilFaults.PAA_SYSTEM_ERROR,
      "paaSILChiediAvvisiPendenti non è una operazione supportata",
      FaultBean::new,
      PaaSILChiediAvvisiPendentiRisposta::setFault
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediPosizioniAperte")
  @ResponsePayload
  public PaaSILChiediPosizioniAperteRisposta paaSILChiediPosizioniAperte(
    @RequestPayload PaaSILChiediPosizioniAperte request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return FaultUtils.setFaultOnResponse(
      new PaaSILChiediPosizioniAperteRisposta(),
      SilFaults.PAA_SYSTEM_ERROR,
      "paaSILChiediPosizioniAperte non è una operazione supportata",
      FaultBean::new,
      PaaSILChiediPosizioniAperteRisposta::setFault
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediStoricoPagamenti")
  @ResponsePayload
  public PaaSILChiediStoricoPagamentiRisposta paaSILChiediStoricoPagamenti(
    @RequestPayload PaaSILChiediStoricoPagamenti request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return FaultUtils.setFaultOnResponse(
      new PaaSILChiediStoricoPagamentiRisposta(),
      SilFaults.PAA_SYSTEM_ERROR,
      "paaSILChiediStoricoPagamenti non è una operazione supportata",
      FaultBean::new,
      PaaSILChiediStoricoPagamentiRisposta::setFault
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILRegistraPagamento")
  @ResponsePayload
  public PaaSILRegistraPagamentoRisposta paaSILRegistraPagamento(
    @RequestPayload PaaSILRegistraPagamento request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return FaultUtils.setFaultOnResponse(
      new PaaSILRegistraPagamentoRisposta(),
      SilFaults.PAA_SYSTEM_ERROR,
      "paaSILRegistraPagamento non è una operazione supportata",
      FaultBean::new,
      PaaSILRegistraPagamentoRisposta::setFault
    );
  }

  private <T extends Risposta> T notAuthorizedFaultResponse(Supplier<T> responseSupplier) {
    T response = responseSupplier.get();
    FaultUtils.setFaultOnResponse(response,
      SilFaults.PAA_ENTE_NON_VALIDO,
      "Utente non autorizzato",
      FaultBean::new,
      T::setFault
    );
    return response;
  }

  private <T extends Risposta> T systemErrorFaultResponse(Supplier<T> responseSupplier, Exception e) {
    log.error("System error occurred", e);
    T response = responseSupplier.get();
    FaultUtils.setFaultOnResponse(response,
      SilFaults.PAA_SYSTEM_ERROR,
      "Errore di sistema",
      FaultBean::new,
      T::setFault
    );
    return response;
  }

  private String getOrganizationIpaCodeFromHeader(SoapHeaderElement header, String operationName) {
    IntestazionePPT intestazionePPT = SoapUtils.unmarshallHeader(header, IntestazionePPT.class);
    String orgIpaCode = Optional.ofNullable(intestazionePPT)
      .map(IntestazionePPT::getCodIpaEnte)
      .orElse(null);
    log.info("processing {} orgIpaCode[{}]", operationName, orgIpaCode);
    return orgIpaCode;
  }

}
