package it.gov.pagopa.pu.sil.registry;

import lombok.Getter;

@Getter
@SuppressWarnings("java:S115") // Suppressing constant naming warning: this is required to match with the api name
public enum RegistryEventType {

  //API "opzione 1" backward compatible with MyPay - MyPivot
  paaSILAutorizzaImportFlusso(true),
  paaSILChiediStatoImportFlusso(true),
  paaSILImportaDovuto(true),
  paaSILPrenotaExportFlusso(true),
  paaSILPrenotaExportFlussoIncrementaleConRicevuta(true),
  paaSILChiediStatoExportFlusso(true),
  paaSILInviaDovuti(true),
  paaSILVerificaAvviso(true),
  paaSILChiediPagati(true),
  paaSILChiediPagatiConRicevuta(true),
  paaSILInviaCarrelloDovuti(true),
  paaSILChiediEsitoCarrelloDovuti(true),
  pivotSILAutorizzaImportFlusso(true),
  pivotSILChiediStatoImportFlusso(true),
  pivotSILAutorizzaImportFlussoTesoreria(true),
  pivotSILChiediStatoImportFlussoTesoreria(true),
  pivotSILPrenotaExportFlussoRiconciliazione(true),
  pivotSILChiediStatoExportFlussoRiconciliazione(true),
  pivotSILChiediAccertamento(true),
  attualizzazioneImporti(false),
  notificaPagamento(false);

  private final boolean exposedByPU;

  RegistryEventType(boolean exposedByPU) {
    this.exposedByPU = exposedByPU;
  }

}
