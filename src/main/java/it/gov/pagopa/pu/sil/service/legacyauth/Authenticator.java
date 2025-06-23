package it.gov.pagopa.pu.sil.service.legacyauth;

public interface Authenticator<T, R> {
    R doAuthentication(T config);
}

