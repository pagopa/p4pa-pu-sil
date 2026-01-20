package it.gov.pagopa.pu.sil.enums;

import lombok.Getter;

import java.util.Optional;

@Getter
public enum ErrorCode {
  USER_UNAUTHORIZED("Utente non autorizzato");

  private final String message;

  ErrorCode(String message) {
    this.message = message;
  }

  ErrorCode() {
    this.message = null;
  }

  public static Optional<ErrorCode> fromCode(String code) {
    if (code == null || code.isEmpty()) {
      return Optional.empty();
    }

    try {
      return Optional.of(ErrorCode.valueOf(code.toUpperCase()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
