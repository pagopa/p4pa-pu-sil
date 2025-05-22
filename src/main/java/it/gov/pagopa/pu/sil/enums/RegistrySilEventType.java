package it.gov.pagopa.pu.sil.enums;

@SuppressWarnings("java:S115") // Suppressing constant naming warning: this is required to match with the api name
public enum RegistrySilEventType {
  //API "opzione 1" backward compatible with MyPay - MyPivot
  paaSILAutorizzaImportFlusso,
  paaSILChiediStatoImportFlusso,
  paaSILImportaDovuto,
  paaSILPrenotaExportFlusso,
  paaSILPrenotaExportFlussoIncrementaleConRicevuta,
  paaSILChiediStatoExportFlusso,
  paaSILInviaDovuti,
  paaSILVerificaAvviso,
  paaSILChiediPagati,
  paaSILChiediPagatiConRicevuta,
  paaSILInviaCarrelloDovuti,
  paaSILChiediEsitoCarrelloDovuti,
  pivotSILAutorizzaImportFlusso,
  pivotSILChiediStatoImportFlusso,
  pivotSILAutorizzaImportFlussoTesoreria,
  pivotSILChiediStatoImportFlussoTesoreria,
  pivotSILPrenotaExportFlussoRiconciliazione,
  pivotSILChiediStatoExportFlussoRiconciliazione,
  pivotSILChiediAccertamento,
  attualizzazioneImporti,
  notificaPagamento
}
