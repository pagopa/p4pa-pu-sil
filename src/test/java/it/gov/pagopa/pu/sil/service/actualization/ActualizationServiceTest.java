package it.gov.pagopa.pu.sil.service.actualization;

import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyActualizationService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.SilAccessTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class ActualizationServiceTest {
  @Mock
  private OrgSilServiceComponent orgSilServiceComponentMock;
  @Mock
  private LegacyActualizationService legacyActualizationServiceMock;
  @Mock
  private SilAccessTokenService silAccessTokenServiceMock;

  private ActualizationService service;

  @BeforeEach
  void setUp() {
    service = new ActualizationService(orgSilServiceComponentMock, legacyActualizationServiceMock, silAccessTokenServiceMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(
      orgSilServiceComponentMock,
      legacyActualizationServiceMock,
      silAccessTokenServiceMock);
  }

  @Test
  void whenActualizeWithNoErrorCodeThenReturnsAmountUpdatesDTO() {
    Long orgSilServiceId = 1L;
    String orgFiscalCode = "FISCALCODE";
    String nav = "30123456789";
    Long organizationId = 2L;
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser(organizationId, orgFiscalCode, "IPACODE");
    String token = "token";
    String silAccessToken = "silAccessToken";
    OrgSilServiceDTO orgSilService = new OrgSilServiceDTO()
      .organizationId(organizationId)
      .orgSilServiceId(orgSilServiceId)
      .applicationName("TestService")
      .flagLegacy(true)
      .serviceUrl("http://service.url");
    ActualizationResultDTO amountUpdatesDTO = new ActualizationResultDTO()
      .errorCode(null);

    Mockito.when(silAccessTokenServiceMock.getSilAccessToken(orgFiscalCode, nav, loggedUser, orgSilService, token)).thenReturn(silAccessToken);
    Mockito.when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, token)).thenReturn(Optional.of(orgSilService));
    Mockito.when(legacyActualizationServiceMock.actualization(Mockito.eq(orgFiscalCode), Mockito.eq(orgSilService), Mockito.eq(loggedUser), Mockito.eq(silAccessToken), Mockito.any(Pagamento.class))).thenReturn(amountUpdatesDTO);

    ActualizationResultDTO result = service.actualize(orgSilServiceId, nav, loggedUser, token);
    assertEquals(amountUpdatesDTO, result);
    assertNull(result.getErrorCode());
  }

  @Test
  void whenOrgSilServiceNotFoundThenThrowsException() {
    Long orgSilServiceId = 1L;
    String nav = "NAV123";
    UserInfo loggedUser = Mockito.mock(UserInfo.class);
    String accessToken = "token";

    Mockito.when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, accessToken)).thenReturn(Optional.empty());

    Assertions.assertThrows(IllegalArgumentException.class, () ->
      service.actualize(orgSilServiceId, nav, loggedUser, accessToken)
    );
  }
}
