package it.gov.pagopa.pu.sil.service.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.Token;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilService;
import it.gov.pagopa.pu.sil.dto.generated.AmountUpdatesDTO;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyActualizationService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

  private ActualizationService service;

  @BeforeEach
  void setUp() {
    service = new ActualizationService(orgSilServiceComponentMock, legacyActualizationServiceMock);
  }

  @Test
  void whenActualizeWithNoErrorCodeThenReturnsAmountUpdatesDTO() {
    Long orgSilServiceId = 1L;
    String orgFiscalCode = "FISCALCODE";
    String nav = "NAV123";
    UserInfo loggedUser = Mockito.mock(UserInfo.class);
    String accessToken = "token";
    Long organizationId = 2L;
    OrgSilService orgSilService = new OrgSilService()
      .flagLegacy(true)
      .serviceUrl("http://service.url");
    Mockito.when(orgSilService.getServiceUrl()).thenReturn("http://service.url");
    AmountUpdatesDTO amountUpdatesDTO = new AmountUpdatesDTO()
      .errorCode(null);

    Mockito.when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, accessToken)).thenReturn(Optional.of(orgSilService));
    Mockito.when(legacyActualizationServiceMock.actualization(Mockito.any(), Mockito.any(), Mockito.any(Pagamento.class))).thenReturn(amountUpdatesDTO);

    try (MockedStatic<AuthorizationService> authService = Mockito.mockStatic(AuthorizationService.class)) {
      authService.when(() -> AuthorizationService.getOrganizationIdFromOrgFiscalCode(loggedUser, orgFiscalCode)).thenReturn(organizationId);
      authService.when(() -> AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser)).thenReturn(null);
      AmountUpdatesDTO result = service.actualize(orgSilServiceId, orgFiscalCode, nav, loggedUser, accessToken);
      assertEquals(amountUpdatesDTO, result);
      assertNull(result.getErrorCode());
    }
  }

  @Test
  void whenActualizeWithErrorCodeNot004ThenIsBlockingErrorFalse() {
    Long orgSilServiceId = 1L;
    String orgFiscalCode = "FISCALCODE";
    String nav = "NAV123";
    UserInfo loggedUser = Mockito.mock(UserInfo.class);
    String accessToken = "token";
    Long organizationId = 2L;
    OrgSilService orgSilService = new OrgSilService()
      .flagLegacy(true)
      .serviceUrl("http://service.url");
    Mockito.when(orgSilService.getServiceUrl()).thenReturn("http://service.url");
    AmountUpdatesDTO amountUpdatesDTO = new AmountUpdatesDTO()
      .errorCode("001");

    Mockito.when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, accessToken)).thenReturn(Optional.of(orgSilService));
    Mockito.when(legacyActualizationServiceMock.actualization(Mockito.any(), Mockito.any(), Mockito.any(Pagamento.class))).thenReturn(amountUpdatesDTO);

    try (MockedStatic<AuthorizationService> authService = Mockito.mockStatic(AuthorizationService.class)) {
      authService.when(() -> AuthorizationService.getOrganizationIdFromOrgFiscalCode(loggedUser, orgFiscalCode)).thenReturn(organizationId);
      authService.when(() -> AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser)).thenReturn(null);
      AmountUpdatesDTO result = service.actualize(orgSilServiceId, orgFiscalCode, nav, loggedUser, accessToken);
      assertEquals(amountUpdatesDTO, result);
      assertFalse(result.getIsBlockingError());
    }
  }

  @Test
  void whenActualizeWithErrorCode004ThenIsBlockingErrorTrue() {
    Long orgSilServiceId = 1L;
    String orgFiscalCode = "FISCALCODE";
    String nav = "NAV123";
    UserInfo loggedUser = Mockito.mock(UserInfo.class);
    String accessToken = "token";
    Long organizationId = 2L;
    OrgSilService orgSilService = new OrgSilService()
      .flagLegacy(true)
      .serviceUrl("http://service.url");
    Mockito.when(orgSilService.getServiceUrl()).thenReturn("http://service.url");
    AmountUpdatesDTO amountUpdatesDTO = new AmountUpdatesDTO()
      .errorCode("004");
    Mockito.when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, accessToken)).thenReturn(Optional.of(orgSilService));
    Mockito.when(legacyActualizationServiceMock.actualization(Mockito.any(), Mockito.any(), Mockito.any(Pagamento.class))).thenReturn(amountUpdatesDTO);

    try (MockedStatic<AuthorizationService> authService = Mockito.mockStatic(AuthorizationService.class)) {
      authService.when(() -> AuthorizationService.getOrganizationIdFromOrgFiscalCode(loggedUser, orgFiscalCode)).thenReturn(organizationId);
      authService.when(() -> AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser)).thenReturn(null);
      AmountUpdatesDTO result = service.actualize(orgSilServiceId, orgFiscalCode, nav, loggedUser, accessToken);
      assertEquals(amountUpdatesDTO, result);
      assertTrue(result.getIsBlockingError());
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
      authService.when(() -> AuthorizationService.getOrganizationIdFromOrgFiscalCode(loggedUser, orgFiscalCode)).thenReturn(organizationId);
      authService.when(() -> AuthorizationService.validateUserForOrganizationId(organizationId, loggedUser)).thenReturn(null);
      Assertions.assertThrows(IllegalArgumentException.class, () ->
        service.actualize(orgSilServiceId, orgFiscalCode, nav, loggedUser, accessToken)
      );
    }
  }

}
