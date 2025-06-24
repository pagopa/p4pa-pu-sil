package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyJwtAuthConfig;
import org.springframework.stereotype.Service;

@Service
public class SilLegacyJwtAuthService {
  public Object authenticate(SilServiceLegacyJwtAuthConfig config) {
    // TODO: Implement JWT authentication logic in the future
    throw new UnsupportedOperationException("JWT authentication not implemented yet");
  }
}
