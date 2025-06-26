package it.gov.pagopa.pu.sil.service.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.dto.generated.AmountUpdatesDTO;
import it.gov.pagopa.pu.sil.service.AccessTokenService;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyActualizationService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ActualizationServiceTest {
  @Mock
  private OrgSilServiceComponent orgSilServiceComponentMock;
  @Mock
  private LegacyActualizationService legacyActualizationServiceMock;
  @Mock
  private AccessTokenService accessTokenServiceMock;

  private ActualizationService service;

  @BeforeEach
  void setUp() {
    service = new ActualizationService(orgSilServiceComponentMock, legacyActualizationServiceMock, accessTokenServiceMock);
  }

  @Test
  void whenActualizeWithNoErrorCodeThenReturnsAmountUpdatesDTO() {
    Long orgSilServiceId = 1L;
    String orgFiscalCode = "FISCALCODE";
    String nav = "NAV123";
    UserInfo loggedUser = Mockito.mock(UserInfo.class);
    Long organizationId = 2L;
    String token = "token";
    AccessToken accessToken = new AccessToken()
      .accessToken("token")
      .tokenType("Bearer");
    OrgSilService orgSilService = new OrgSilService()
      .organizationId(organizationId)
      .orgSilServiceId(orgSilServiceId)
      .flagLegacy(true)
      .serviceUrl("http://service.url");
    AmountUpdatesDTO amountUpdatesDTO = new AmountUpdatesDTO()
      .errorCode(null);

    Mockito.when(accessTokenServiceMock.getAccessToken(orgSilService, token)).thenReturn(accessToken.getAccessToken());
    Mockito.when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilService.getOrgSilServiceId(), accessToken.getAccessToken())).thenReturn(Optional.of(orgSilService));
    Mockito.when(legacyActualizationServiceMock.actualization(Mockito.any(), Mockito.any(), Mockito.any(Pagamento.class))).thenReturn(amountUpdatesDTO);

    try (MockedStatic<AuthorizationService> authService = Mockito.mockStatic(AuthorizationService.class)) {
      authService.when(() -> AuthorizationService.validateUserForOrganizationId(orgSilService.getOrganizationId(), loggedUser)).thenAnswer(Answers.RETURNS_DEFAULTS);
      authService.when(() -> AuthorizationService.getOrgFiscalCodeFromUserInfo(loggedUser, orgSilService.getOrganizationId())).thenReturn(orgFiscalCode);
      AmountUpdatesDTO result = service.actualize(orgSilServiceId, nav, loggedUser, token);
      assertEquals(amountUpdatesDTO, result);
      assertNull(result.getErrorCode());
    }
  }

  @Test
  void whenOrgSilServiceNotFoundThenThrowsException() {
    Long orgSilServiceId = 1L;
    String orgFiscalCode = "FISCALCODE";
    String nav = "NAV123";
    UserInfo loggedUser = Mockito.mock(UserInfo.class);
    String accessToken = "token";
    Long organizationId = 2L;

    Mockito.when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, accessToken)).thenReturn(Optional.empty());

    try (MockedStatic<AuthorizationService> authService = Mockito.mockStatic(AuthorizationService.class)) {
      authService.when(() -> AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser)).thenAnswer(Answers.RETURNS_DEFAULTS);
      authService.when(() -> AuthorizationService.getOrgFiscalCodeFromUserInfo(loggedUser, organizationId)).thenReturn(orgFiscalCode);
      Assertions.assertThrows(IllegalArgumentException.class, () ->
        service.actualize(orgSilServiceId, nav, loggedUser, accessToken)
      );
    }
  }
}
