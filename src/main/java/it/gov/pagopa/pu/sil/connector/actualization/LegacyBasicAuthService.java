package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;

public interface LegacyBasicAuthService {
  Token login(Credentials credentials, String authUrl);
}
