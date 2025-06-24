package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyJwtAuthConfig;
import it.gov.pagopa.pu.sil.dto.LegacyTokenDTO;
import org.springframework.stereotype.Service;

@Service
public class SilLegacyJwtAuthService {
  public LegacyTokenDTO authenticate(SilServiceLegacyJwtAuthConfig config) {
    // TODO: Implement JWT authentication logic in the future
    throw new UnsupportedOperationException("JWT authentication not implemented yet");
  }
}
