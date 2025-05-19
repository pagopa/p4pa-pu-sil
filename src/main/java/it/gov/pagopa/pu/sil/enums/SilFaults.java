package it.gov.pagopa.pu.sil.enums;

public enum SilFaults {

  PAA_SYSTEM_ERROR("PAA_SYSTEM_ERROR");

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
