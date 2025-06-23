package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyJwtAuthConfig;
import it.gov.pagopa.pu.sil.service.legacyauth.JwtAuthConfigService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class JwtAuthConfigServiceTest {
    private final JwtAuthConfigService service = new JwtAuthConfigService();

    @Test
    void doAuthentication_shouldThrowUnsupportedOperationException() {
      SilServiceLegacyJwtAuthConfig config = mock(SilServiceLegacyJwtAuthConfig.class);
        assertThrows(UnsupportedOperationException.class,
          () -> service.doAuthentication(config),
          "JWT authentication not implemented yet");
    }
}

