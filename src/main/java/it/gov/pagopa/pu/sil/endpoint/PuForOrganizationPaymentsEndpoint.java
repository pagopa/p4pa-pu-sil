package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.util.soap.FaultUtils;
import it.gov.pagopa.pu.sil.util.soap.SoapUtils;
import it.veneto.regione.pagamenti.ente.*;
import it.veneto.regione.pagamenti.ente.ppthead.IntestazionePPT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.server.endpoint.annotation.SoapHeader;

@Endpoint
@Slf4j
public class PuForOrganizationPaymentsEndpoint {
  public static final String NAMESPACE_URI = "http://www.regione.veneto.it/pagamenti/ente/";
  public static final String NAME = "PagamentiTelematiciDovutiPagati";

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILAutorizzaImportFlusso")
  @ResponsePayload
  public PaaSILAutorizzaImportFlussoRisposta paaSILAutorizzaImportFlusso(
      @RequestPayload PaaSILAutorizzaImportFlusso request,
      @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header){
    IntestazionePPT intestazionePPT = SoapUtils.unmarshallHeader(header, IntestazionePPT.class);
    log.info("processing paaSILAutorizzaImportFlusso codIpaEnte[{}]", intestazionePPT.getCodIpaEnte());
    PaaSILAutorizzaImportFlussoRisposta response = new PaaSILAutorizzaImportFlussoRisposta();

    //check if the logged user has the right to call this endpoint
    if(!SecurityUtils.isValidWsSilUser(intestazionePPT.getCodIpaEnte())){
      log.error("User [{}] not authorized to call paaSILAutorizzaImportFlusso for organization {}",
        SecurityUtils.getLoggedUser().getUserId() ,intestazionePPT.getCodIpaEnte());
      return FaultUtils.setFaultOnResponse(
        response,
        SilFaults.PAA_ENTE_NON_VALIDO,
        "Utente non autorizzato",
        intestazionePPT.getCodIpaEnte(),
        FaultBean::new,
        PaaSILAutorizzaImportFlussoRisposta::setFault
      );
    }

    //TODO: implement the logic to handle the SOAP Action P4ADEV-2892
    return FaultUtils.setFaultOnResponse(
      response,
      SilFaults.PAA_SYSTEM_ERROR,
      intestazionePPT.getCodIpaEnte(),
      intestazionePPT.getCodIpaEnte(),
      FaultBean::new,
      PaaSILAutorizzaImportFlussoRisposta::setFault
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILChiediAvvisiPendenti")
  @ResponsePayload
  public PaaSILChiediAvvisiPendentiRisposta paaSILChiediAvvisiPendenti(
      @RequestPayload PaaSILChiediAvvisiPendenti request,
      @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return FaultUtils.setFaultOnResponse(
      new PaaSILChiediAvvisiPendentiRisposta(),
      SilFaults.PAA_SYSTEM_ERROR,
      "paaSILChiediAvvisiPendenti is an UNSUPPORTED Operation",
      request.getCodIpaEnte(),
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
      "paaSILChiediPosizioniAperte is an UNSUPPORTED Operation",
      request.getCodIpaEnte(),
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
      "paaSILChiediStoricoPagamenti is an UNSUPPORTED Operation",
      request.getCodIpaEnte(),
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
      "paaSILRegistraPagamento is an UNSUPPORTED Operation",
      request.getCodIpaEnte(),
      FaultBean::new,
      PaaSILRegistraPagamentoRisposta::setFault
    );
  }
}
