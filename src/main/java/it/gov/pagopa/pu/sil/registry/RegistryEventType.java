package it.gov.pagopa.pu.sil.registry;

import lombok.Getter;

@Getter
@SuppressWarnings("java:S115") // Suppressing constant naming warning: this is required to match with the api name
public enum RegistryEventType {

  //API "opzione 1" backward compatible with MyPay - MyPivot
  PTDP_paaSILAutorizzaImportFlusso(true),
  PTDP_paaSILImportaDovuto(true),
  PTDP_paaSILInviaDovuti(true),
  PTDP_paaSILVerificaAvviso(true),
  PTDP_paaSILInviaCarrelloDovuti(true),
  PTDP_paaSILChiediPosizioniAperte(true),

  PTPR_pivotSILAutorizzaImportFlusso(true),
  PTPR_pivotSILAutorizzaImportFlussoTesoreria(true),

  SIL_attualizzazioneImporti(false),
  SIL_notificaPagamento(false);

  private final boolean exposedByPU;

  RegistryEventType(boolean exposedByPU) {
    this.exposedByPU = exposedByPU;
  }

}
