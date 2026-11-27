package it.gov.pagopa.pu.sil.connector.sil.actualization;

import it.gov.pagopa.sil.actualizationlegacy.dto.generated.Credentials;
import it.gov.pagopa.sil.actualizationlegacy.dto.generated.Token;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;

public interface LegacyBasicAuthService {
  Token login(String orgFiscalCode, String orgSilServiceName, String nav, UserInfo loggedUser, Credentials credentials, String authUrl);
}
