package it.gov.pagopa.pu.sil.enums.legacy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum ExportFileLegacyFileVersion {
  v1_0("1.0"),
  v1_1("1.1"),
  v1_2("1.2"),
  v1_3("1.3"),
  ;

  private final String legacyValue;

  ExportFileLegacyFileVersion(String legacyValue) {
    this.legacyValue = legacyValue;
  }

  public String getLegacyValue() { return legacyValue; }

}
