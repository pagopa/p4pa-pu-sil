package it.gov.pagopa.pu.sil.enums;

public enum SilFaults {

  PAA_SYSTEM_ERROR("PAA_SYSTEM_ERROR"),
  PAA_ENTE_NON_VALIDO("Ente non valido o utente non autorizzato"),
  PAA_IUV_NON_VALIDO("IUV non valido"),
  PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO("Identificativo tipo dovuto non valido"),
  PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO("Identificativo tipo dovuto non abilitato"),
  PAA_IUD_DUPLICATO("IUD duplicato"),
  PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA("Marca da bollo digitale non valida"),
  PAA_IMPORTO_SINGOLO_VERSAMENTO_NON_VALIDO("Importo versamento non valido"),
  PAA_DATI_SPECIFICI_RISCOSSIONE_NON_VALIDO("Dati specifici riscossione non validi"),
  PAA_IMPORTO_BILANCIO_NON_VALIDO("Importo bilancio non valido"),
  PAA_CAUSALE_NON_PRESENTE("Causale non presente o non valida"),
  PAA_ANAGRAFICA_NON_VALIDA("Anagrafica debitore non valida"),
  PAA_XML_NON_VALIDO("XML non valido"),
  PAA_CODICE_FISCALE_NON_VALIDO("Codice fiscale non valido"),
  PAA_LIMITE_MASSIMO_DOVUTI_MULTIBENEFICIARI("Numero massimo versamenti enti secondari superato"),
  PAA_ENTE_SECONDARIO_NON_VALIDO("Ente secondario non valido"),
  PAA_LIMITE_MASSIMO_DOVUTI_CARRELLO("Numero massimo dovuti nel carrello superato"),
  PAA_ERRORE_RECUPERO_DOVUTI_ENTI_SECONDARI("Errore recupero dovuti enti secondari"),

  PIVOT_SYSTEM_ERROR("PIVOT_SYSTEM_ERROR"),
  PIVOT_ENTE_NON_VALIDO("Ente non valido o utente non autorizzato"),
  PIVOT_TIPO_FLUSSO_NON_VALIDO("Tipo di flusso tesoreria non valido");

  private final String description;

  private SilFaults(String description) {
    this.description = description;
  }

  public String description() {
    return this.description;
  }

  public String code() {
    return this.name();
  }
}
