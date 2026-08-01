package it.gov.pagopa.pu.sil.endpoint;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile;
import it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum;
import it.gov.pagopa.pu.registries.dto.generated.RegistryOutcome;
import it.gov.pagopa.pu.sil.dto.generated.ExportStatusResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.ImportFileResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.ImportStatusResponseDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.enums.legacy.ExportFileLegacyStatus;
import it.gov.pagopa.pu.sil.enums.legacy.IngestionFlowFileLegacyStatus;
import it.gov.pagopa.pu.sil.exception.soap.PuForOrganizationReconciliationExceptionHandler;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;
import it.gov.pagopa.pu.sil.registry.RegistryEventType;
import it.gov.pagopa.pu.sil.registry.RegistryLogger;
import it.gov.pagopa.pu.sil.security.SecurityUtils;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.exportfile.ExportFileProcessingStatusService;
import it.gov.pagopa.pu.sil.service.exportfile.PivotSILPrenotaExportFlussoRiconciliazioneService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileAuthorizationService;
import it.gov.pagopa.pu.sil.service.ingestionflowfile.IngestionFlowFileProcessingStatusService;
import it.gov.pagopa.pu.sil.service.queryassessments.LegacyQueryAssessmentsService;
import it.gov.pagopa.pu.sil.util.soap.SoapUtils;
import it.veneto.regione.pagamenti.pivot.ente.*;
import it.veneto.regione.pagamenti.pivot.ente.ppthead.IntestazionePPT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.server.endpoint.annotation.SoapHeader;

@Endpoint
@Slf4j
@RequiredArgsConstructor
public class PuForOrganizationReconciliationEndpoint {
  public static final String NAMESPACE_URI = "http://www.regione.veneto.it/pagamenti/pivot/ente/";
  public static final String NAME = "PagamentiTelematiciPagatiRiconciliati";

  private final RegistryLogger registryLogger;
  private final PuForOrganizationReconciliationExceptionHandler exceptionHandler;

  private final IngestionFlowFileAuthorizationService ingestionFlowFileAuthorizationService;
  private final IngestionFlowFileProcessingStatusService ingestionFlowFileProcessingStatusService;
  private final PivotSILPrenotaExportFlussoRiconciliazioneService pivotSILPrenotaExportFlussoRiconciliazioneService;
  private final ExportFileProcessingStatusService exportFileProcessingStatusService;
  private final LegacyQueryAssessmentsService queryAssessmentsService;

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILChiediAccertamento")
  @ResponsePayload
  public PivotSILChiediAccertamentoRisposta pivotSILChiediAccertamento(
    @RequestPayload PivotSILChiediAccertamento request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "pivotSILChiediAccertamento");
    try {
      return queryAssessmentsService.handlePivotSILChiediAccertamento(
        userInfo,
        accessToken,
        orgIpaCode,
        request
      );
    } catch (Exception e) {
      return exceptionHandler.setExceptionTranscoded(
        new PivotSILChiediAccertamentoRisposta(),
        PivotSILChiediAccertamentoRisposta::setFault,
        e
      );
    }
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILChiediStatoExportFlussoRiconciliazione")
  @ResponsePayload
  public PivotSILChiediStatoExportFlussoRiconciliazioneRisposta pivotSILChiediStatoExportFlussoRiconciliazione(
    @RequestPayload PivotSILChiediStatoExportFlussoRiconciliazione request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "pivotSILChiediStatoExportFlussoRiconciliazione");
    try {
      Pair<ExportStatusResponseDTO.StatusEnum, String> processingStatus = exportFileProcessingStatusService.getProcessingStatus(
        userInfo,
        accessToken,
        orgIpaCode,
        Long.valueOf(request.getRequestToken()),
        ExportFile.ExportFileTypeEnum.CLASSIFICATIONS);
      PivotSILChiediStatoExportFlussoRiconciliazioneRisposta response = new PivotSILChiediStatoExportFlussoRiconciliazioneRisposta();
      response.setStato(ExportFileLegacyStatus.fromValue2LegacyValue(processingStatus.getLeft()));
      response.setDownloadUrl(processingStatus.getRight());
      return response;
    } catch (Exception e) {
      return exceptionHandler.setExceptionTranscoded(
        new PivotSILChiediStatoExportFlussoRiconciliazioneRisposta(),
        PivotSILChiediStatoExportFlussoRiconciliazioneRisposta::setFault,
        e);
    }
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

    try {
      ImportStatusResponseDTO processingStatusDTO = ingestionFlowFileProcessingStatusService.getProcessingStatus(
        userInfo,
        accessToken,
        orgIpaCode,
        Long.valueOf(request.getRequestToken()),
        IngestionFlowFileTypeEnum.TREASURY_OPI,
        IngestionFlowFileTypeEnum.TREASURY_CSV,
        IngestionFlowFileTypeEnum.TREASURY_XLS,
        IngestionFlowFileTypeEnum.TREASURY_POSTE);
      PivotSILChiediStatoImportFlussoTesoreriaRisposta response = new PivotSILChiediStatoImportFlussoTesoreriaRisposta();
      response.setStato(IngestionFlowFileLegacyStatus.fromValue2LegacyValue(processingStatusDTO.getStatus()));
      return response;
    } catch (Exception e) {
      return exceptionHandler.setExceptionTranscoded(
        new PivotSILChiediStatoImportFlussoTesoreriaRisposta(),
        PivotSILChiediStatoImportFlussoTesoreriaRisposta::setFault,
        e);
    }
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

    try {
      ImportStatusResponseDTO processingStatusDTO = ingestionFlowFileProcessingStatusService.getProcessingStatus(
        userInfo,
        accessToken,
        orgIpaCode,
        Long.valueOf(request.getRequestToken()),
        IngestionFlowFileTypeEnum.PAYMENT_NOTIFICATION);
      PivotSILChiediStatoImportFlussoRisposta response = new PivotSILChiediStatoImportFlussoRisposta();
      response.setStato(IngestionFlowFileLegacyStatus.fromValue2LegacyValue(processingStatusDTO.getStatus()));
      return response;
    } catch (Exception e) {
      return exceptionHandler.setExceptionTranscoded(
        new PivotSILChiediStatoImportFlussoRisposta(),
        PivotSILChiediStatoImportFlussoRisposta::setFault,
        e);
    }
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
      .eventType(RegistryEventType.PTPR_pivotSILAutorizzaImportFlussoTesoreria)
      .loggedUser(userInfo)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> {
        ImportFileResponseDTO result = ingestionFlowFileAuthorizationService.authorizeTreasuryIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          request.getTipoFlusso());
        PivotSILAutorizzaImportFlussoTesoreriaRisposta response = new PivotSILAutorizzaImportFlussoTesoreriaRisposta();
        response.setRequestToken(result.getImportId());
        response.setUploadUrl(result.getUploadUrl());
        response.setAuthorizationToken(result.getAuthorizationToken());
        response.setImportPath(result.getImportPath());
        return Triple.of(response, null, RegistryOutcome.OK);
      },
      exceptionHandler.buildExceptionHandlerFunction(
        new PivotSILAutorizzaImportFlussoTesoreriaRisposta(),
        PivotSILAutorizzaImportFlussoTesoreriaRisposta::setFault
      )
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILAutorizzaImportFlusso")
  @ResponsePayload
  public PivotSILAutorizzaImportFlussoRisposta pivotSILAutorizzaImportFlusso(
    @RequestPayload PivotSILAutorizzaImportFlusso request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    UserInfo userInfo = SecurityUtils.getLoggedUser();
    String accessToken = SecurityUtils.getAccessToken();
    String orgIpaCode = SoapUtils.getOrganizationIpaCodeFromHeader(header,
      IntestazionePPT.class,
      IntestazionePPT::getCodIpaEnte,
      "pivotSILAutorizzaImportFlusso");

    RegistryContextData contextData = RegistryContextData.builder()
      .orgFiscalCode(AuthorizationService.getOrgFiscalCodeFromUserInfo(userInfo, orgIpaCode))
      .eventType(RegistryEventType.PTPR_pivotSILAutorizzaImportFlusso)
      .loggedUser(userInfo)
      .build();

    return registryLogger.execute(
      contextData,
      request,
      () -> {
        ImportFileResponseDTO result = ingestionFlowFileAuthorizationService.authorizeIngestionFlowFile(
          userInfo,
          accessToken,
          orgIpaCode,
          IngestionFlowFileTypeEnum.PAYMENT_NOTIFICATION);
        PivotSILAutorizzaImportFlussoRisposta response = new PivotSILAutorizzaImportFlussoRisposta();
        response.setRequestToken(result.getImportId());
        response.setUploadUrl(result.getUploadUrl());
        response.setAuthorizationToken(result.getAuthorizationToken());
        response.setImportPath(result.getImportPath());
        return Triple.of(response, null, RegistryOutcome.OK);
      },
      exceptionHandler.buildExceptionHandlerFunction(
        new PivotSILAutorizzaImportFlussoRisposta(),
        PivotSILAutorizzaImportFlussoRisposta::setFault
      )
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

    PivotSILPrenotaExportFlussoRiconciliazioneRisposta response;
    try {
      response = pivotSILPrenotaExportFlussoRiconciliazioneService.doReservation(
        userInfo,
        accessToken,
        orgIpaCode,
        request
      );
    } catch (Exception e) {
      response = exceptionHandler.setExceptionTranscoded(
        new PivotSILPrenotaExportFlussoRiconciliazioneRisposta(),
        PivotSILPrenotaExportFlussoRiconciliazioneRisposta::setFault,
        e);
    }
    return response;
  }


  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILChiediPagatiRiconciliati")
  @ResponsePayload
  public PivotSILChiediPagatiRiconciliatiRisposta pivotSILChiediPagatiRiconciliati(
    @RequestPayload PivotSILChiediPagatiRiconciliati request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return exceptionHandler.setFault(
      new PivotSILChiediPagatiRiconciliatiRisposta(),
      PivotSILChiediPagatiRiconciliatiRisposta::setFault,
      SilFaults.PIVOT_SYSTEM_ERROR,
      "paaSILRegistraPagamento non è una operazione supportata"
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILAutorizzaImportFlussoRendicontazione")
  @ResponsePayload
  public PivotSILAutorizzaImportFlussoRendicontazioneRisposta pivotSILAutorizzaImportFlussoRendicontazione(
    @RequestPayload PivotSILAutorizzaImportFlussoRendicontazione request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return exceptionHandler.setFault(
      new PivotSILAutorizzaImportFlussoRendicontazioneRisposta(),
      PivotSILAutorizzaImportFlussoRendicontazioneRisposta::setFault,
      SilFaults.PIVOT_SYSTEM_ERROR,
      "pivotSILAutorizzaImportFlussoRendicontazione non è una operazione supportata"
    );
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "pivotSILAutorizzaImportFlussoRT")
  @ResponsePayload
  public PivotSILAutorizzaImportFlussoRTRisposta pivotSILAutorizzaImportFlussoRT(
    @RequestPayload PivotSILAutorizzaImportFlussoRT request,
    @SoapHeader("{http://www.regione.veneto.it/pagamenti/pivot/ente/ppthead}intestazionePPT") SoapHeaderElement header) {
    return exceptionHandler.setFault(
      new PivotSILAutorizzaImportFlussoRTRisposta(),
      PivotSILAutorizzaImportFlussoRTRisposta::setFault,
      SilFaults.PIVOT_SYSTEM_ERROR,
      "pivotSILAutorizzaImportFlussoRT non è una operazione supportata"
    );
  }

}

