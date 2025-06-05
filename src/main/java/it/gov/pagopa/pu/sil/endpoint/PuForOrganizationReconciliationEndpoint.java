package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileRequestDTO.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.sil.enums.RegistrySilEventType;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.SilOutcome;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileAuthorizationService;
import it.gov.pagopa.pu.sil.util.soap.FaultUtils;
import it.gov.pagopa.pu.sil.util.soap.SoapUtils;
import it.veneto.regione.pagamenti.pivot.ente.ppthead.IntestazionePPT;
import it.veneto.regione.pagamenti.pivot.ente.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.server.endpoint.annotation.SoapHeader;

import java.util.function.Supplier;
import java.util.Set;

@Endpoint
@Slf4j
public class PuForOrganizationReconciliationEndpoint {
  public static final String NAMESPACE_URI = "http://www.regione.veneto.it/pagamenti/pivot/ente/";
  public static final String NAME = "PagamentiTelematiciPagatiRiconciliati";

  private final RegistryLogger registryLogger;

  private final IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationService;

  public PuForOrganizationReconciliationEndpoint(RegistryLogger registryLogger,
                                                 IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationService) {
    this.registryLogger = registryLogger;
    this.ingestionFlowFileAuthorizationService = ingestionFlowFileAuthorizationService;
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILAutorizzaImportFlussoTesoreria")
  @ResponsePayload
  public PivotSILAutorizzaImportFlussoTesoreriaRisposta pivotSILAutorizzaImportFlussoTesoreria(
    @RequestPayload PivotSILAutorizzaImportFlussoTesoreria request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "pivotSILAutorizzaImportFlussoTesoreria");

    IngestionFlowFileTypeEnum type = IngestionFlowFileTypeEnum.valueOf(request.getTipoFlusso());
    if (!Set.of(
        IngestionFlowFileTypeEnum.TREASURY_OPI,
        IngestionFlowFileTypeEnum.TREASURY_CSV,
        IngestionFlowFileTypeEnum.TREASURY_XLS,
        IngestionFlowFileTypeEnum.TREASURY_POSTE)
      .contains(type)) {
      return FaultUtils.setFaultOnResponse(
        new PivotSILAutorizzaImportFlussoTesoreriaRisposta(),
        SilFaults.PIVOT_TIPO_FLUSSO_NON_VALIDO,
        "Tipo di flusso tesoreria non valido",
        FaultBean::new,
        PivotSILAutorizzaImportFlussoTesoreriaRisposta::setFault
      );
    }

    return registryLogger.execute(
      AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode),
      RegistrySilEventType.pivotSILAutorizzaImportFlussoTesoreria,
      null,
      request,
      userInfo,
      null,
      () -> {
        Pair<Long, String> result = ingestionFlowFileAuthorizationService.authorizeIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          type);
        PivotSILAutorizzaImportFlussoTesoreriaRisposta response = new PivotSILAutorizzaImportFlussoTesoreriaRisposta();
        response.setRequestToken(String.valueOf(result.getLeft()));
        response.setUploadUrl(result.getRight());
        return Triple.of(response, null, SilOutcome.OK);
      },
      FaultUtils.unauthorizedExceptionHandler(
        (Supplier<PivotSILAutorizzaImportFlussoTesoreriaRisposta>) PivotSILAutorizzaImportFlussoTesoreriaRisposta::new,
        PivotSILAutorizzaImportFlussoTesoreriaRisposta::setFault,
        FaultBean::new,
        SilFaults.PIVOT_ENTE_NON_VALIDO,
        SilFaults.PIVOT_SYSTEM_ERROR
      ),
      null,
      null
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILAutorizzaImportFlusso")
  @ResponsePayload
  public PivotSILAutorizzaImportFlussoRisposta pivotSILAutorizzaImportFlusso(
    @RequestPayload PivotSILAutorizzaImportFlusso request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header){
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "pivotSILAutorizzaImportFlusso");

    return registryLogger.execute(
      AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode),
      RegistrySilEventType.pivotSILAutorizzaImportFlusso,
      null,
      request,
      userInfo,
      null,
      () -> {
        Pair<Long, String> result = ingestionFlowFileAuthorizationService.authorizeIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          IngestionFlowFileTypeEnum.PAYMENT_NOTIFICATION);
        PivotSILAutorizzaImportFlussoRisposta response = new PivotSILAutorizzaImportFlussoRisposta();
        response.setRequestToken(String.valueOf(result.getLeft()));
        response.setUploadUrl(result.getRight());
        return Triple.of(response, null, SilOutcome.OK);
      },
      FaultUtils.unauthorizedExceptionHandler(
        (Supplier<PivotSILAutorizzaImportFlussoRisposta>) PivotSILAutorizzaImportFlussoRisposta::new,
        PivotSILAutorizzaImportFlussoRisposta::setFault,
        FaultBean::new,
        SilFaults.PIVOT_ENTE_NON_VALIDO,
        SilFaults.PIVOT_SYSTEM_ERROR
      ),
      null,
      null
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

