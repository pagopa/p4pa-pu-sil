package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.util.soap.FaultUtils;
import it.gov.pagopa.pu.sil.util.soap.SoapUtils;
import it.veneto.regione.pagamenti.pivot.ente.*;
import it.veneto.regione.pagamenti.pivot.ente.ppthead.IntestazionePPT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.server.endpoint.annotation.SoapHeader;

import java.util.Optional;

@Endpoint
@Slf4j
public class PuForOrganizationReconciliationEndpoint {
  public static final String NAMESPACE_URI = "http://www.regione.veneto.it/pagamenti/pivot/ente/";
  public static final String NAME = "PagamentiTelematiciPagatiRiconciliati";

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILAutorizzaImportFlusso")
  @ResponsePayload
  public PivotSILAutorizzaImportFlussoRisposta pivotSILAutorizzaImportFlusso(
    @RequestPayload PivotSILAutorizzaImportFlusso request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header){

    IntestazionePPT intestazionePPT = SoapUtils.unmarshallHeader(header, IntestazionePPT.class);
    log.info("processing PivotSILAutorizzaImportFlusso codIpaEnte[{}]", Optional.ofNullable(intestazionePPT).map(IntestazionePPT::getCodIpaEnte).orElse(null));


    return FaultUtils.setFaultOnResponse(
      new PivotSILAutorizzaImportFlussoRisposta(),
      SilFaults.PIVOT_SYSTEM_ERROR,
      Optional.ofNullable(intestazionePPT).map(IntestazionePPT::getCodIpaEnte).orElse(null),
      FaultBean::new,
      PivotSILAutorizzaImportFlussoRisposta::setFault
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILChiediPagatiRiconciliati")
  @ResponsePayload
  public PivotSILChiediPagatiRiconciliatiRisposta pivotSILChiediPagatiRiconciliati(
      @RequestPayload PivotSILChiediPagatiRiconciliati request,
      @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return FaultUtils.setFaultOnResponse(
      new PivotSILChiediPagatiRiconciliatiRisposta(),
      SilFaults.PIVOT_SYSTEM_ERROR,
      "paaSILRegistraPagamento non è una operazione supportata",
      FaultBean::new,
      PivotSILChiediPagatiRiconciliatiRisposta::setFault
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILAutorizzaImportFlussoRendicontazione")
  @ResponsePayload
  public PivotSILAutorizzaImportFlussoRendicontazioneRisposta pivotSILAutorizzaImportFlussoRendicontazione(
      @RequestPayload PivotSILAutorizzaImportFlussoRendicontazione request,
      @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return FaultUtils.setFaultOnResponse(
      new PivotSILAutorizzaImportFlussoRendicontazioneRisposta(),
      SilFaults.PIVOT_SYSTEM_ERROR,
      "pivotSILAutorizzaImportFlussoRendicontazione non è una operazione supportata",
      FaultBean::new,
      PivotSILAutorizzaImportFlussoRendicontazioneRisposta::setFault
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILAutorizzaImportFlussoRT")
  @ResponsePayload
  public PivotSILAutorizzaImportFlussoRTRisposta pivotSILAutorizzaImportFlussoRT(
      @RequestPayload PivotSILAutorizzaImportFlussoRT request,
      @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return FaultUtils.setFaultOnResponse(
      new PivotSILAutorizzaImportFlussoRTRisposta(),
      SilFaults.PIVOT_SYSTEM_ERROR,
      "pivotSILAutorizzaImportFlussoRT non è una operazione supportata",
      FaultBean::new,
      PivotSILAutorizzaImportFlussoRTRisposta::setFault
    );
  }
}

