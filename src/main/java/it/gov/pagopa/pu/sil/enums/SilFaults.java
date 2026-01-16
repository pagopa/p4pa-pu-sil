package it.gov.pagopa.pu.sil.enums;

import java.util.Arrays;

public enum SilFaults {

  PAA_SYSTEM_ERROR("PAA_SYSTEM_ERROR"),
  PAA_ENTE_NON_VALIDO("Ente non valido o utente non autorizzato", "INVALID_ORGANIZATION", "INVALID_ORGANIZATION_STATUS"),
  PAA_IUV_NON_VALIDO("IUV non valido", "INVALID_IUV", "MISSING_IUV"),
  PAA_IUD_NON_VALIDO("IUD non valido"),
  PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO("Identificativo tipo dovuto non valido", "MISSING_DEBT_POSITION_TYPE_ORG"),
  PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO("Identificativo tipo dovuto non abilitato", "DEBT_POSITION_TYPE_ORG_UNAUTHORIZED"),
  PAA_IUD_DUPLICATO("IUD duplicato"),
  PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA("Marca da bollo digitale non valida"),
  PAA_IMPORTO_SINGOLO_VERSAMENTO_NON_VALIDO("Importo versamento non valido", "INVALID_AMOUNT", "INVALID_CENTS_AMOUNT"),
  PAA_DATI_SPECIFICI_RISCOSSIONE_NON_VALIDO("Dati specifici riscossione non validi", "INVALID_TAXONOMY_CATEGORY", "MISSING_TAXONOMY_CATEGORY", "INVALID_LEGACY_PAYMENT_METADATA"),
  PAA_IMPORTO_BILANCIO_NON_VALIDO("Importo bilancio non valido", "INVALID_BALANCE"),
  PAA_CAUSALE_NON_PRESENTE("Causale non presente o non valida", "MISSING_REMITTANCE_INFORMATION"),
  PAA_ANAGRAFICA_NON_VALIDA("Anagrafica debitore non valida", "INVALID_FULLNAME", "INVALID_EMAIL", "MISSING_DEBTOR"),
  PAA_XML_NON_VALIDO("XML non valido"),
  PAA_CODICE_FISCALE_NON_VALIDO("Codice fiscale non valido", "INVALID_VAT_CODE"),
  PAA_VERSIONE_TRACCIATO_NON_VALIDA("Versione tracciato non valida"),
  PAA_DATE_FROM_NON_VALIDO("Data inizio non valida"),
  PAA_DATE_TO_NON_VALIDO("Data fine non valida"),
  PAA_INTERVALLO_DATE_NON_VALIDO("L'intervallo data inizio e data fine non è valido"),
  PAA_LIMITE_MASSIMO_DOVUTI_MULTIBENEFICIARI("Numero massimo versamenti enti secondari superato", "TOO_MANY_TRANSFERS_FOR_INSTALLMENT"),
  PAA_ENTE_SECONDARIO_NON_VALIDO("Ente secondario non valido", "INVALID_IBAN"),
  PAA_LIMITE_MASSIMO_DOVUTI_CARRELLO("Numero massimo dovuti nel carrello superato"),
  PAA_ERRORE_RECUPERO_DOVUTI_ENTI_SECONDARI("Errore recupero dovuti enti secondari"),
  PAA_URL_NON_VALIDA("Url risposta pagamento non valida"),
  PAA_ID_SESSION_NON_VALIDO("ID session non valido"),
  PAA_PAGAMENTO_NON_INIZIATO("Pagamento non effettuato"),
  PAA_PAGAMENTO_SCADUTO("Pagamento scaduto"),
  PAA_DOVUTO_NON_PAGABILE("Dovuto non pagabile"),
  PAA_AZIONE_NON_VALIDA("Azione non valida"),
  PAA_LIMITAZIONE_ENTI_SECONDARI_ERROR("Limite massimo dovuti enti secondari superato"),
  PAA_IMPORT_DOVUTO_NON_PRESENTE("Dovuto non presente"),
  PAA_CAMPO_NON_MODIFICABILE("Campo non modificabile", "IMMUTABLE_FIELD"),
  PAA_REQUEST_TOKEN_NON_VALIDO("requestToken non valido"),
  PAA_DATA_ESECUZIONE_PAGAMENTO_NON_VALIDA("dataEsecuzionePagamento non presente o non valida", "MISSING_DUE_DATE", "INVALID_DUE_DATE"),
  PAA_DOVUTO_DUPLICATO("Dovuto già esistente", "INSTALLMENT_ALREADY_EXISTS", "DEBT_POSITION_ALREADY_EXISTS"),

  PIVOT_SYSTEM_ERROR("PIVOT_SYSTEM_ERROR"),
  PIVOT_ENTE_NON_VALIDO("Ente non valido o utente non autorizzato"),
  PIVOT_VERSIONE_TRACCIATO_NON_VALIDA("Versione tracciato non valida"),
  PIVOT_INTERVALLO_DATE_NON_VALIDO("L'intervallo data inizio e data fine non è valido"),
  PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO("Identificativo tipo dovuto non valido"),
  PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO("Identificativo tipo dovuto non abilitato"),
  PIVOT_TIPO_FLUSSO_NON_VALIDO("Tipo di flusso tesoreria non valido"),
  PIVOT_BOLLETTA_NON_TROVATA("Bolletta non trovata"),
  PIVOT_NESSUNA_RENDICONTAZIONE_TROVATA("Nessuna rendicontazione associata allo IUF della bolletta"),
  PIVOT_RICHIESTA_CON_PARAMETRI_MULTIPLI("Richiesta con parametri multipli")
  ;

  private final String description;
  private final String[] nativeFaults;

  SilFaults(String description, String... nativeFaults) {
    this.description = description;
    this.nativeFaults = nativeFaults;
  }

  public String description() {
    return this.description;
  }
  public String[] nativeFaults() { return this.nativeFaults; }

  public String code() {
    return this.name();
  }

  public static SilFaults fromNativeFault2LegacyCode(String nativeFault) {
    return Arrays.stream(SilFaults.values())
      .filter(fault -> Arrays.stream(fault.nativeFaults)
        .anyMatch(nativeFault::contains))
      .findFirst()
      .orElseThrow(()-> new IllegalArgumentException("Unexpected value %s".formatted(nativeFault)));
  }
}
