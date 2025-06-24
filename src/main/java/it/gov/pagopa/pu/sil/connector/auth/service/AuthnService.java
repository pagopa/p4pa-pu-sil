package it.gov.pagopa.pu.sil.connector.auth.service;

import it.gov.pagopa.pu.auth.dto.generated.AccessToken;

public interface AuthnService {
  AccessToken postToken(String clientId, String grantType, String scope,
                        String subjectToken, String subjectIssuer,
                        String subjectTokenType, String clientSecret);
}
