package it.gov.pagopa.pu.sil.service;

import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyBasicAuthService;
import it.gov.pagopa.actualization.legacy.dto.generated.Credentials;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.sil.service.legacyauth.BasicAuthConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicAuthConfigServiceTest {
    @Mock
    private LegacyBasicAuthService legacyBasicAuthServiceMock;

    private BasicAuthConfigService service;

    @BeforeEach
    void setUp() {
        service = new BasicAuthConfigService(legacyBasicAuthServiceMock);
    }

    @Test
    void doAuthentication_shouldCallLegacyBasicAuthServiceAndReturnToken() {
      OrgSilService orgSilService = mock(OrgSilService.class);
        Token expectedToken = new Token();
        when(legacyBasicAuthServiceMock.login(any(Credentials.class), eq("http://auth.url"))).thenReturn(expectedToken);

        Token result = service.getAuthConfig(orgSilService).doAuthentication();

        assertEquals(expectedToken, result);
        ArgumentCaptor<Credentials> credentialsCaptor = ArgumentCaptor.forClass(Credentials.class);
        verify(legacyBasicAuthServiceMock).login(credentialsCaptor.capture(), eq("http://auth.url"));
        Credentials creds = credentialsCaptor.getValue();
        assertEquals("user", creds.getUsername());
        assertEquals("pass", creds.getPassword());
    }
}

