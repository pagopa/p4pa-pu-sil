package it.gov.pagopa.pu.sil.service.actualization;

import it.gov.pagopa.actualization.dto.generated.Error;
import it.gov.pagopa.actualization.dto.generated.Payment;
import it.gov.pagopa.actualization.dto.generated.UpdatedPayment;
import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.actualization.ActualizationService;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyActualizationService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import it.gov.pagopa.pu.sil.exception.*;
import it.gov.pagopa.pu.sil.mapper.AmountUpdatesMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.SilAccessTokenService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpServerErrorException;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActualizationFacadeServiceTest {
  @Mock
  private OrgSilServiceComponent orgSilServiceComponentMock;
  @Mock
  private LegacyActualizationService legacyActualizationServiceMock;
  @Mock
  private ActualizationService actualizationServiceMock;
  @Mock
  private SilAccessTokenService silAccessTokenServiceMock;
  @Mock
  private AmountUpdatesMapper amountUpdatesMapperMock;

  private ActualizationFacadeService service;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    service = new ActualizationFacadeService(orgSilServiceComponentMock, legacyActualizationServiceMock, actualizationServiceMock, silAccessTokenServiceMock, amountUpdatesMapperMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      orgSilServiceComponentMock,
      legacyActualizationServiceMock,
      actualizationServiceMock,
      silAccessTokenServiceMock,
      amountUpdatesMapperMock);
  }

  @ParameterizedTest
  @ValueSource(strings = {"happyCase", "002", "003", "004"})
  void legacyActualizeTest(String testCase) {
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
    PagamentoAggiornato updatedPayment = podamFactory.manufacturePojo(PagamentoAggiornato.class);
    ActualizationResultDTO actualizationResultDTO = podamFactory.manufacturePojo(ActualizationResultDTO.class);
    if (testCase.equals("happyCase")) {
      updatedPayment.codice(null).dettaglio(null);
      when(amountUpdatesMapperMock.pagamentoAggiornato2AmountUpdatesDTO(updatedPayment)).thenReturn(actualizationResultDTO);
    } else {
      updatedPayment.codice(PagamentoAggiornato.CodiceEnum.fromValue(testCase)).dettaglio("errore " + testCase);
    }

    when(silAccessTokenServiceMock.getSilAccessToken(orgFiscalCode, nav, loggedUser, orgSilService, token)).thenReturn(silAccessToken);
    when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, token)).thenReturn(Optional.of(orgSilService));
    when(legacyActualizationServiceMock.actualization(Mockito.eq(orgFiscalCode), Mockito.eq(orgSilService), Mockito.eq(loggedUser), Mockito.eq(silAccessToken), Mockito.any(Pagamento.class))).thenReturn(updatedPayment);


    switch (testCase) {
      case "happyCase" -> {
        ActualizationResultDTO result = service.actualize(orgSilServiceId, nav, loggedUser, token);
        assertEquals(actualizationResultDTO, result);
      }
      case "002" ->
        Assertions.assertThrows(PaymentNotFoundException.class, () -> service.actualize(orgSilServiceId, nav, loggedUser, token));
      case "003" ->
        Assertions.assertThrows(PaymentNotNotifiedException.class, () -> service.actualize(orgSilServiceId, nav, loggedUser, token));
      case "004" ->
        Assertions.assertThrows(PaymentInvalidStatusException.class, () -> service.actualize(orgSilServiceId, nav, loggedUser, token));
      default -> Assertions.fail("unexpected test case " + testCase);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"happyCase", "002", "003", "004", "genericError"})
  void nativeActualizeTest(String testCase) {
    Long orgSilServiceId = 1L;
    String orgFiscalCode = "FISCALCODE";
    String nav = "30123456789";
    Long organizationId = 2L;
    UserInfo loggedUser = AuthorizationServiceTest.buildAdminUser(organizationId, orgFiscalCode, "IPACODE");
    String token = "token";
    OrgSilServiceDTO orgSilService = new OrgSilServiceDTO()
      .organizationId(organizationId)
      .orgSilServiceId(orgSilServiceId)
      .applicationName("TestService")
      .flagLegacy(false)
      .serviceUrl("http://service.url");
    UpdatedPayment updatedPayment = podamFactory.manufacturePojo(UpdatedPayment.class);
    ActualizationResultDTO actualizationResultDTO = podamFactory.manufacturePojo(ActualizationResultDTO.class);

    when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, token)).thenReturn(Optional.of(orgSilService));
    if (testCase.equals("happyCase")) {
      when(actualizationServiceMock.actualization(Mockito.eq(orgSilService), Mockito.eq(loggedUser), Mockito.eq(token), Mockito.any(Payment.class))).thenReturn(updatedPayment);
      when(amountUpdatesMapperMock.updatedPayment2AmountUpdatesDTO(updatedPayment)).thenReturn(actualizationResultDTO);
    } else {
      HttpServerErrorException mockedException = mock(HttpServerErrorException.class);
      when(mockedException.getResponseBodyAs(Error.class)).thenReturn(Error.builder().code(testCase).message("error " + testCase).build());
      when(actualizationServiceMock.actualization(Mockito.eq(orgSilService), Mockito.eq(loggedUser), Mockito.eq(token), Mockito.any(Payment.class))).thenThrow(mockedException);
    }

    switch (testCase) {
      case "happyCase" -> {
        ActualizationResultDTO result = service.actualize(orgSilServiceId, nav, loggedUser, token);
        assertEquals(actualizationResultDTO, result);
      }
      case "002" -> Assertions.assertThrows(PaymentNotFoundException.class, () -> service.actualize(orgSilServiceId, nav, loggedUser, token));
      case "003" -> Assertions.assertThrows(PaymentNotNotifiedException.class, () -> service.actualize(orgSilServiceId, nav, loggedUser, token));
      case "004" -> Assertions.assertThrows(PaymentInvalidStatusException.class, () -> service.actualize(orgSilServiceId, nav, loggedUser, token));
      case "genericError" -> Assertions.assertThrows(IllegalStateBusinessException.class, () -> service.actualize(orgSilServiceId, nav, loggedUser, token));
      default -> Assertions.fail("unexpected test case " + testCase);
    }
  }

}
