package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.connector.pagopa.checkout.client.CheckoutClient;
import it.gov.pagopa.pu.sil.mapper.CartRequestMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPosizioniAperte;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPosizioniAperteRisposta;
import it.veneto.regione.pagamenti.ente.PaaSILPosizioniAperte;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtIdentificativoUnivocoPersonaFG;
import it.veneto.regione.schemas._2012.pagamenti.ente.Dovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.StTipoIdentificativoUnivocoPersFG;
import jakarta.activation.DataHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaaSILChiediPosizioniAperteServiceTest {

  @Mock private DebtPositionService debtPositionServiceMock;
  @Mock private OrganizationService organizationServiceMock;
  @Mock private JAXBTransformService jaxbTransformServiceMock;
  @Mock private CheckoutClient checkoutClientMock;
  @Mock private CartRequestMapper cartRequestMapperMock;
  @Mock private DebtPositionTypeService debtPositionTypeServiceMock;

  private PaaSILChiediPosizioniAperteService service;

  @BeforeEach
  void setUp() {
    service = new PaaSILChiediPosizioniAperteService(
      debtPositionServiceMock,
      organizationServiceMock,
      jaxbTransformServiceMock,
      checkoutClientMock,
      cartRequestMapperMock,
      debtPositionTypeServiceMock
    );
  }

  @Test
  void givenValidRequestWhenProcessRequestThenBuildsOpenPositions() {
    // Given
    String accessToken = "token";
    String orgIpaCode = "IPA123";
    long orgId = 42L;

    PaaSILChiediPosizioniAperte request = new PaaSILChiediPosizioniAperte();
    request.setCodIpaEnte(orgIpaCode);

    CtIdentificativoUnivocoPersonaFG idFG = new CtIdentificativoUnivocoPersonaFG();
    idFG.setCodiceIdentificativoUnivoco("RSSMRA80A01H501U");
    idFG.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.F);
    request.setIdentificativoUnivocoPersonaFG(idFG);

    UserInfo userInfo = new UserInfo();

    Organization org = new Organization();
    org.setOrganizationId(orgId);
    org.setOrgName("Comune di Test");
    org.setStatus(OrganizationStatus.ACTIVE);

    InstallmentDTO installment = new InstallmentDTO();
    PersonDTO debtor = new PersonDTO();
    debtor.setFiscalCode("RSSMRA80A01H501U");
    debtor.setFullName("Mario Rossi");
    debtor.setAddress("Via Roma 1");
    debtor.setCivic("1");
    debtor.setPostalCode("00100");
    debtor.setLocation("Roma");
    debtor.setProvince("RM");
    debtor.setNation("IT");
    debtor.setEmail("mario.rossi@example.com");
    debtor.setEntityType(PersonEntityType.F);
    installment.setDebtor(debtor);
    installment.setIuv("IUV123");

    PaymentOptionDTO paymentOption = new PaymentOptionDTO();
    paymentOption.setPaymentOptionType(PaymentOptionTypeEnum.DOWN_PAYMENT); // use a valid value from your enum
    paymentOption.setInstallments(List.of(installment));

    DebtPositionDTO dp = new DebtPositionDTO();
    dp.setPaymentOptions(List.of(paymentOption));

    when(organizationServiceMock.getOrganizationById(eq(orgId), eq(accessToken)))
      .thenReturn(Optional.of(org));

    when(debtPositionServiceMock.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
      anyString(), any(PersonEntityType.class), eq(orgId), eq(InstallmentStatus.UNPAID), eq(null), eq(null), eq(accessToken))
    ).thenReturn(List.of(dp));

    byte[] marshalledBytes = "dovuti-xml".getBytes();
    when(jaxbTransformServiceMock.marshallingAsBytes(any(Dovuti.class), eq(Dovuti.class)))
      .thenReturn(marshalledBytes);

    // When
    PaaSILChiediPosizioniAperteRisposta response;
    try (MockedStatic<AuthorizationService> mockedAuth = Mockito.mockStatic(AuthorizationService.class)) {
      mockedAuth.when(() -> AuthorizationService.validateAdminRole(eq(orgIpaCode), eq(userInfo)))
        .thenAnswer(inv -> null);
      mockedAuth.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode)))
        .thenReturn(orgId);

      response = service.processRequest(request, userInfo, accessToken, orgIpaCode);
    }

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getPaaSILPosizioniApertes());
    Assertions.assertEquals(1, response.getPaaSILPosizioniApertes().size());

    PaaSILPosizioniAperte item = response.getPaaSILPosizioniApertes().get(0);
    Assertions.assertEquals(orgIpaCode, item.getCodIpaEnte());
    Assertions.assertEquals("Comune di Test", item.getDeNomeEnte());
    DataHandler dh = item.getDovuti();
    Assertions.assertNotNull(dh, "Dovuti DataHandler should be set");

    verify(organizationServiceMock).getOrganizationById(orgId, accessToken);
    verify(debtPositionServiceMock).getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
      eq("RSSMRA80A01H501U"), any(PersonEntityType.class), eq(orgId), eq(InstallmentStatus.UNPAID), eq(null), eq(null), eq(accessToken)
    );
    verify(jaxbTransformServiceMock, times(1)).marshallingAsBytes(any(Dovuti.class), eq(Dovuti.class));
  }
}
