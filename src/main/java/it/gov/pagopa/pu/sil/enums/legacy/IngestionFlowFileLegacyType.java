package it.gov.pagopa.pu.sil.enums.legacy;

public enum IngestionFlowFileLegacyType {
  TREASURY_OPI("O"),
  TREASURY_CSV("C"),
  TREASURY_XLS("T"),
  TREASURY_POSTE("Y"),
  ;

  private final String value;

  IngestionFlowFileLegacyType(String value) {
    this.value = value;
  }

  public String getValue() { return this.value; }

  public String getCode(){ return this.name(); }
}
