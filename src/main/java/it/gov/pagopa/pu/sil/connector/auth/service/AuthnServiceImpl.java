package it.gov.pagopa.pu.sil.connector.auth.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.sil.connector.auth.client.AuthnClient;
import org.springframework.stereotype.Service;

@Service
public class AuthnServiceImpl implements AuthnService {
  private final AuthnClient authnClient;

  public AuthnServiceImpl(AuthnClient authnClient) {
    this.authnClient = authnClient;
  }

  @Override
  public AccessToken postToken(String clientId, String grantType, String scope, String subjectToken, String subjectIssuer, String subjectTokenType, String clientSecret) {
    return authnClient.postToken(clientId, grantType, scope, subjectToken, subjectIssuer, subjectTokenType, clientSecret);
  }
}
