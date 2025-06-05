package it.gov.pagopa.pu.sil.enums;

public enum SilFaults {

  PAA_SYSTEM_ERROR("PAA_SYSTEM_ERROR"),
  PAA_ENTE_NON_VALIDO("Ente non valido o utente non autorizzato"),

  PIVOT_SYSTEM_ERROR("PIVOT_SYSTEM_ERROR"),
  PIVOT_ENTE_NON_VALIDO("Ente non valido o utente non autorizzato"),
  PIVOT_TIPO_FLUSSO_NON_VALIDO("Tipo di flusso tesoreria non valido")
    ;

  private final String description;

  private SilFaults(String description) {
    this.description = description;
  }

  public String description() {
    return this.description;
  }

  public String code(){
    return this.name();
  }
}
