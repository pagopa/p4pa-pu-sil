package it.gov.pagopa.pu.sil.connector.auth;

public interface AuthnService {
  String getAccessToken(String orgIpaCode);
}
