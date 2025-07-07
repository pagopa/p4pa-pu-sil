package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.registry.RegistryContextData;

public interface LegacyBasicAuthService {
  Token login(RegistryContextData contextData, Credentials credentials, String authUrl);
}
