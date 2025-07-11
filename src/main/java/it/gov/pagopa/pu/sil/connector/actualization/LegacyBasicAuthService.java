package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;

public interface LegacyBasicAuthService {
  Token login(String orgFiscalCode, String orgSilServiceName, String nav, UserInfo loggedUser, Credentials credentials, String authUrl);
}
