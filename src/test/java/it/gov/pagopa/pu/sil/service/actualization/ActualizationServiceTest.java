package it.gov.pagopa.pu.sil.service.actualization;

import it.gov.pagopa.actualization.dto.generated.Error;
import it.gov.pagopa.actualization.dto.generated.Payment;
import it.gov.pagopa.actualization.dto.generated.UpdatedPayment;
import it.gov.pagopa.actualization.legacy.dto.generated.Pagamento;
import it.gov.pagopa.actualization.legacy.dto.generated.PagamentoAggiornato;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.actualization.LegacyActualizationService;
import it.gov.pagopa.pu.sil.connector.actualization.NativeActualizationService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrgSilServiceComponent;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.PaymentInvalidStatusException;
import it.gov.pagopa.pu.sil.exception.PaymentNotFoundException;
import it.gov.pagopa.pu.sil.exception.PaymentNotNotifiedException;
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

@ExtendWith(MockitoExtension.class)
class ActualizationServiceTest {
  @Mock
  private OrgSilServiceComponent orgSilServiceComponentMock;
  @Mock
  private LegacyActualizationService legacyActualizationServiceMock;
  @Mock
  private NativeActualizationService nativeActualizationServiceMock;
  @Mock
  private SilAccessTokenService silAccessTokenServiceMock;
  @Mock
  private AmountUpdatesMapper amountUpdatesMapperMock;

  private ActualizationService service;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    service = new ActualizationService(orgSilServiceComponentMock, legacyActualizationServiceMock, nativeActualizationServiceMock, silAccessTokenServiceMock, amountUpdatesMapperMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      orgSilServiceComponentMock,
      legacyActualizationServiceMock,
      nativeActualizationServiceMock,
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
      Mockito.when(amountUpdatesMapperMock.pagamentoAggiornato2AmountUpdatesDTO(updatedPayment)).thenReturn(actualizationResultDTO);
    } else {
      updatedPayment.codice(PagamentoAggiornato.CodiceEnum.fromValue(testCase)).dettaglio("errore " + testCase);
    }

    Mockito.when(silAccessTokenServiceMock.getSilAccessToken(orgFiscalCode, nav, loggedUser, orgSilService, token)).thenReturn(silAccessToken);
    Mockito.when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, token)).thenReturn(Optional.of(orgSilService));
    Mockito.when(legacyActualizationServiceMock.actualization(Mockito.eq(orgFiscalCode), Mockito.eq(orgSilService), Mockito.eq(loggedUser), Mockito.eq(silAccessToken), Mockito.any(Pagamento.class))).thenReturn(updatedPayment);


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

    Mockito.when(orgSilServiceComponentMock.getOrgSilServiceById(orgSilServiceId, token)).thenReturn(Optional.of(orgSilService));
    if (testCase.equals("happyCase")) {
      Mockito.when(nativeActualizationServiceMock.actualization(Mockito.eq(orgSilService), Mockito.eq(loggedUser), Mockito.eq(token), Mockito.any(Payment.class))).thenReturn(updatedPayment);
      Mockito.when(amountUpdatesMapperMock.updatedPayment2AmountUpdatesDTO(updatedPayment)).thenReturn(actualizationResultDTO);
    } else {
      HttpServerErrorException mockedException = Mockito.mock(HttpServerErrorException.class);
      Mockito.when(mockedException.getResponseBodyAs(Error.class)).thenReturn(Error.builder().code(testCase).message("error " + testCase).build());
      Mockito.when(nativeActualizationServiceMock.actualization(Mockito.eq(orgSilService), Mockito.eq(loggedUser), Mockito.eq(token), Mockito.any(Payment.class))).thenThrow(mockedException);
    }

    switch (testCase) {
      case "happyCase" -> {
        ActualizationResultDTO result = service.actualize(orgSilServiceId, nav, loggedUser, token);
        assertEquals(actualizationResultDTO, result);
      }
      case "002" -> {
        Assertions.assertThrows(PaymentNotFoundException.class, () -> service.actualize(orgSilServiceId, nav, loggedUser, token));
      }
      case "003" -> {
        Assertions.assertThrows(PaymentNotNotifiedException.class, () -> service.actualize(orgSilServiceId, nav, loggedUser, token));
      }
      case "004" -> {
        Assertions.assertThrows(PaymentInvalidStatusException.class, () -> service.actualize(orgSilServiceId, nav, loggedUser, token));
      }
      case "genericError" -> {
        Assertions.assertThrows(ApplicationException.class, () -> service.actualize(orgSilServiceId, nav, loggedUser, token));
      }
      default -> Assertions.fail("unexpected test case " + testCase);
    }
  }

}
