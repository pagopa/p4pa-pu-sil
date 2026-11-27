package it.gov.pagopa.pu.sil.connector.sil.actualization;

import it.gov.pagopa.sil.actualizationlegacy.dto.generated.Credentials;
import it.gov.pagopa.sil.actualizationlegacy.dto.generated.Token;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.connector.sil.actualization.client.LegacyBasicAuthClient;
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
