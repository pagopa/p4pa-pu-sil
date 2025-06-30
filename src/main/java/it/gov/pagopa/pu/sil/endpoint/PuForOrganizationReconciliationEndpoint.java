package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.exception.ExportFileClientException;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.legacy.IngestionFlowFileLegacyStatus;
import it.gov.pagopa.pu.sil.exception.IngestionFlowFileTypeValidationException;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.exportfile.PivotSILPrenotaExportFlussoRiconciliazioneService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileAuthorizationService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileProcessingStatusService;
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

import java.util.function.Function;
import java.util.function.Supplier;

@Endpoint
@Slf4j
public class PuForOrganizationReconciliationEndpoint {
  public static final String NAMESPACE_URI = "http://www.regione.veneto.it/pagamenti/pivot/ente/";
  public static final String NAME = "PagamentiTelematiciPagatiRiconciliati";

  private final RegistryLogger registryLogger;
  private final IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationService;
  private final IngestionFlowFileProcessingStatusService ingestionFlowFileProcessingStatusService;
  private final PivotSILPrenotaExportFlussoRiconciliazioneService pivotSILPrenotaExportFlussoRiconciliazioneService;

  public PuForOrganizationReconciliationEndpoint(RegistryLogger registryLogger,
                                                 IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationService,
                                                 IngestionFlowFileProcessingStatusService ingestionFlowFileProcessingStatusService,
                                                 PivotSILPrenotaExportFlussoRiconciliazioneService pivotSILPrenotaExportFlussoRiconciliazioneService) {
    this.registryLogger = registryLogger;
    this.ingestionFlowFileAuthorizationService = ingestionFlowFileAuthorizationService;
    this.ingestionFlowFileProcessingStatusService = ingestionFlowFileProcessingStatusService;
    this.pivotSILPrenotaExportFlussoRiconciliazioneService = pivotSILPrenotaExportFlussoRiconciliazioneService;
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILChiediStatoImportFlussoTesoreria")
  @ResponsePayload
  public PivotSILChiediStatoImportFlussoTesoreriaRisposta pivotSILChiediStatoImportFlussoTesoreria(
    @RequestPayload PivotSILChiediStatoImportFlussoTesoreria request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "pivotSILChiediStatoImportFlussoTesoreria");

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode))
      .eventType(RegistryEventType.pivotSILChiediStatoImportFlussoTesoreria)
      .loggedUser(userInfo)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> {
        IngestionFlowFile ingestionFlowFile = ingestionFlowFileProcessingStatusService.getIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          Long.valueOf(request.getRequestToken()),
          IngestionFlowFileTypeEnum.TREASURY_OPI,
          IngestionFlowFileTypeEnum.TREASURY_CSV,
          IngestionFlowFileTypeEnum.TREASURY_XLS,
          IngestionFlowFileTypeEnum.TREASURY_POSTE);
        PivotSILChiediStatoImportFlussoTesoreriaRisposta response = new PivotSILChiediStatoImportFlussoTesoreriaRisposta();
        response.setStato(IngestionFlowFileLegacyStatus.fromValue2LegacyValue(ingestionFlowFile.getStatus()));
        return Triple.of(response, null, RegistryOutcome.OK);
      },
      FaultUtils.unauthorizedOrSystemExceptionHandler(
        new PivotSILChiediStatoImportFlussoTesoreriaRisposta(),
        PivotSILChiediStatoImportFlussoTesoreriaRisposta::setFault,
        FaultBean::new,
        SilFaults.PIVOT_ENTE_NON_VALIDO,
        SilFaults.PIVOT_SYSTEM_ERROR)
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILChiediStatoImportFlusso")
  @ResponsePayload
  public PivotSILChiediStatoImportFlussoRisposta pivotSILChiediStatoImportFlusso(
    @RequestPayload PivotSILChiediStatoImportFlusso request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "pivotSILChiediStatoImportFlusso");

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode))
      .eventType(RegistryEventType.pivotSILChiediStatoImportFlusso)
      .loggedUser(userInfo)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> {
        IngestionFlowFile ingestionFlowFile = ingestionFlowFileProcessingStatusService.getIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          Long.valueOf(request.getRequestToken()),
          IngestionFlowFileTypeEnum.PAYMENT_NOTIFICATION);
        PivotSILChiediStatoImportFlussoRisposta response = new PivotSILChiediStatoImportFlussoRisposta();
        response.setStato(IngestionFlowFileLegacyStatus.fromValue2LegacyValue(ingestionFlowFile.getStatus()));
        return Triple.of(response, null, RegistryOutcome.OK);
      },
      FaultUtils.unauthorizedOrSystemExceptionHandler(
        new PivotSILChiediStatoImportFlussoRisposta(),
        PivotSILChiediStatoImportFlussoRisposta::setFault,
        FaultBean::new,
        SilFaults.PIVOT_ENTE_NON_VALIDO,
        SilFaults.PIVOT_SYSTEM_ERROR)
    );
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

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode))
      .eventType(RegistryEventType.pivotSILAutorizzaImportFlussoTesoreria)
      .loggedUser(userInfo)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> {
        Pair<Long, String> result = ingestionFlowFileAuthorizationService.authorizeTreasuryIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          request.getTipoFlusso());
        PivotSILAutorizzaImportFlussoTesoreriaRisposta response = new PivotSILAutorizzaImportFlussoTesoreriaRisposta();
        response.setRequestToken(String.valueOf(result.getLeft()));
        response.setUploadUrl(result.getRight());
        return Triple.of(response, null, RegistryOutcome.OK);
      },
      this::handleIngestionFlowFileTypeValidationException
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

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode))
      .eventType(RegistryEventType.pivotSILAutorizzaImportFlusso)
      .loggedUser(userInfo)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> {
        Pair<Long, String> result = ingestionFlowFileAuthorizationService.authorizeIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          IngestionFlowFileTypeEnum.PAYMENT_NOTIFICATION);
        PivotSILAutorizzaImportFlussoRisposta response = new PivotSILAutorizzaImportFlussoRisposta();
        response.setRequestToken(String.valueOf(result.getLeft()));
        response.setUploadUrl(result.getRight());
        return Triple.of(response, null, RegistryOutcome.OK);
      },
      FaultUtils.unauthorizedOrSystemExceptionHandler(
        new PivotSILAutorizzaImportFlussoRisposta(),
        PivotSILAutorizzaImportFlussoRisposta::setFault,
        FaultBean::new,
        SilFaults.PIVOT_ENTE_NON_VALIDO,
        SilFaults.PIVOT_SYSTEM_ERROR)
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILPrenotaExportFlussoRiconciliazione")
  @ResponsePayload
  public PivotSILPrenotaExportFlussoRiconciliazioneRisposta pivotSILPrenotaExportFlussoRiconciliazione(
      @RequestPayload PivotSILPrenotaExportFlussoRiconciliazione request,
      @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "pivotSILPrenotaExportFlussoRiconciliazione");

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode))
      .eventType(RegistryEventType.pivotSILPrenotaExportFlussoRiconciliazione)
      .loggedUser(userInfo)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> {
        Long result = pivotSILPrenotaExportFlussoRiconciliazioneService.doReservation(
          userInfo,
          accessToken,
          orgIpaCode,
          request
        );
        PivotSILPrenotaExportFlussoRiconciliazioneRisposta response = new PivotSILPrenotaExportFlussoRiconciliazioneRisposta();
        response.setRequestToken(String.valueOf(result));
        return Triple.of(response, null, RegistryOutcome.OK);
      },
      exportFileExceptionHandler(PivotSILPrenotaExportFlussoRiconciliazioneRisposta::new)
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

  private PivotSILAutorizzaImportFlussoTesoreriaRisposta handleIngestionFlowFileTypeValidationException(Exception e) {
    if (e instanceof IngestionFlowFileTypeValidationException ie) {
      return FaultUtils.setFaultOnResponse(
        new PivotSILAutorizzaImportFlussoTesoreriaRisposta(),
        SilFaults.PIVOT_TIPO_FLUSSO_NON_VALIDO,
        ie.getMessage()
      );
    }
    return FaultUtils.unauthorizedOrSystemExceptionHandler(
      new PivotSILAutorizzaImportFlussoTesoreriaRisposta(),
      PivotSILAutorizzaImportFlussoTesoreriaRisposta::setFault,
      FaultBean::new,
      SilFaults.PIVOT_ENTE_NON_VALIDO,
      SilFaults.PIVOT_SYSTEM_ERROR
    ).apply(e);
  }

  private <T extends Risposta> Function<Exception, T> exportFileExceptionHandler(Supplier<T> response) {
    return (Exception e) -> {
      if (e instanceof ExportFileClientException ce) {
        SilFaults fault = switch (ce.getCode()) {
          case
            ProcessExecutionsErrorDTO.CodeEnum.PROCESS_EXECUTIONS_INVALID_FILE_VERSION ->
            SilFaults.PIVOT_VERSIONE_TRACCIATO_NON_VALIDA;
          case
            ProcessExecutionsErrorDTO.CodeEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE ->
            SilFaults.PIVOT_INTERVALLO_DATE_NON_VALIDO;
          default -> SilFaults.PIVOT_SYSTEM_ERROR;
        };

        return FaultUtils.setFaultOnResponse(
          response.get(),
          fault,
          fault.description()
        );
      }
      if (e instanceof ExportFileServiceException se) {
        return FaultUtils.setFaultOnResponse(
          response.get(),
          se.getFault(),
          se.getFault().description() + ": " + se.getMessage()
        );
      }
      return FaultUtils.unauthorizedOrSystemExceptionHandler(
        response.get(),
        T::setFault,
        FaultBean::new,
        SilFaults.PAA_ENTE_NON_VALIDO,
        SilFaults.PAA_SYSTEM_ERROR
      ).apply(e);
    };
  }
}


