package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.connector.actualization.client.LegacyBasicAuthClient;
import org.springframework.stereotype.Component;

@Component
public class LegacyBasicAuthServiceImpl implements LegacyBasicAuthService {
  private final LegacyBasicAuthClient legacyBasicAuthClient;

  public LegacyBasicAuthServiceImpl(LegacyBasicAuthClient legacyBasicAuthClient) {
    this.legacyBasicAuthClient = legacyBasicAuthClient;
  }

  @Override
  public Token login(String orgFiscalCode, String orgSilServiceName, String nav, UserInfo loggedUser, Credentials credentials, String authUrl) {
    return legacyBasicAuthClient.login(orgFiscalCode, orgSilServiceName, nav, loggedUser, credentials, authUrl);
  }
}
