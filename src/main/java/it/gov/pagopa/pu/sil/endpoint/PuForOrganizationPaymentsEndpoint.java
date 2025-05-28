package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileReservationService;
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

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Endpoint
@Slf4j
public class PuForOrganizationPaymentsEndpoint {
  public static final String NAMESPACE_URI = "http://www.regione.veneto.it/pagamenti/ente/";
  public static final String NAME = "PagamentiTelematiciDovutiPagati";

  private final IngestionFlowFileReservationService ingestionFlowFileReservationService;

  public PuForOrganizationPaymentsEndpoint(IngestionFlowFileReservationService ingestionFlowFileReservationService) {
    this.ingestionFlowFileReservationService = ingestionFlowFileReservationService;
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "paaSILAutorizzaImportFlusso")
  @ResponsePayload
  public PaaSILAutorizzaImportFlussoRisposta paaSILAutorizzaImportFlusso(
      @RequestPayload PaaSILAutorizzaImportFlusso request,
      @SoapHeader("{http://www.regione.veneto.it/pagamenti/ente/ppthead}intestazionePPT") SoapHeaderElement header){
    IntestazionePPT intestazionePPT = SoapUtils.unmarshallHeader(header, IntestazionePPT.class);
    log.info("processing paaSILAutorizzaImportFlusso codIpaEnte[{}]", Optional.ofNullable(intestazionePPT).map(IntestazionePPT::getCodIpaEnte).orElse(null));

    //check if the logged user has the right to call this endpoint
    if(intestazionePPT==null || !SecurityUtils.isAdminUser(intestazionePPT.getCodIpaEnte())){
      log.error("User [{}] not authorized to call paaSILAutorizzaImportFlusso for organization {}",
        Optional.ofNullable(SecurityUtils.getLoggedUser()).map(UserInfo::getUserId).orElse(null),
        Optional.ofNullable(intestazionePPT).map(IntestazionePPT::getCodIpaEnte).orElse(null) );
      return notAuthorizedFaultResponse(PaaSILAutorizzaImportFlussoRisposta::new);
    }

    String accessToken = SecurityUtils.getAccessToken();
    try {
      String uploadUrl = ingestionFlowFileReservationService.uploadUrlGenerator(IngestionFlowFileTypeEnum.DP_INSTALLMENTS, 1L, accessToken);
      PaaSILAutorizzaImportFlussoRisposta response = new PaaSILAutorizzaImportFlussoRisposta();
      response.setRequestToken(intestazionePPT.getCodIpaEnte() + UUID.randomUUID());
      response.setUploadUrl(uploadUrl);
      return response;
    } catch (Exception e) {
      return FaultUtils.setFaultOnResponse(
        new PaaSILAutorizzaImportFlussoRisposta(),
        SilFaults.PAA_SYSTEM_ERROR,
        intestazionePPT.getCodIpaEnte(),
        FaultBean::new,
        PaaSILAutorizzaImportFlussoRisposta::setFault
      );
    }
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

  private <T extends Risposta> T notAuthorizedFaultResponse(Supplier<T> responseSupplier){
    FaultBean fault = new FaultBean();
    fault.setFaultCode(SilFaults.PAA_ENTE_NON_VALIDO.code());
    fault.setFaultString(SilFaults.PAA_ENTE_NON_VALIDO.description());
    fault.setDescription("Utente non autorizzato");
    fault.setId(String.valueOf(System.currentTimeMillis()));
    fault.setSerial(0);
    T response = responseSupplier.get();
    response.setFault(fault);
    return response;
  }
}
