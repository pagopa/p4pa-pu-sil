package it.gov.pagopa.pu.sil.service.legacyauth;

import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyJwtAuthConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SilLegacyJwtAuthServiceTest {
    private SilLegacyJwtAuthService service;

    @BeforeEach
    void setUp() {
        service = new SilLegacyJwtAuthService();
    }

    @Test
    void authenticate_shouldThrowUnsupportedOperationException() {
        SilServiceLegacyJwtAuthConfig config = new SilServiceLegacyJwtAuthConfig();
        assertThrows(UnsupportedOperationException.class, () -> service.authenticate(config));
    }
}

