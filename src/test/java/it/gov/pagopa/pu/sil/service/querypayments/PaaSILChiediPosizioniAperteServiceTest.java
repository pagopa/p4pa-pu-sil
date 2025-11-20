package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.pu.sil.util.Constants.EXCLUDED_DEBT_POSITION_TYPE_CODES;
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

  @ParameterizedTest
  @CsvSource(value = {
    "IPA123, 1",           // With orgIpaCode - single organization path
    "null, 2"              // Without orgIpaCode - broker organizations path (multiple orgs)
  }, nullValues = {"null"})
  void givenValidRequestWhenProcessRequestThenBuildsOpenPositions(String orgIpaCode, int expectedOrgCount) {
    // Given
    String accessToken = "token";
    long orgId1 = 42L;
    long orgId2 = 43L;
    Long brokerId = 100L;
    List<String> debtPositionTypeOrgCodesToExclude = EXCLUDED_DEBT_POSITION_TYPE_CODES;
    OffsetDateTimeIntervalFilter dateFilter = new OffsetDateTimeIntervalFilter(null, null);

    PaaSILChiediPosizioniAperte request = new PaaSILChiediPosizioniAperte();
    request.setCodIpaEnte(orgIpaCode);

    CtIdentificativoUnivocoPersonaFG idFG = new CtIdentificativoUnivocoPersonaFG();
    idFG.setCodiceIdentificativoUnivoco("RSSMRA80A01H501U");
    idFG.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.F);
    request.setIdentificativoUnivocoPersonaFG(idFG);

    UserInfo userInfo = new UserInfo();
    userInfo.setBrokerId(brokerId);

    Organization org1 = new Organization();
    org1.setOrganizationId(orgId1);
    org1.setOrgName("Comune di Test");
    org1.setStatus(OrganizationStatus.ACTIVE);
    org1.setBrokerId(brokerId);

    Organization org2 = new Organization();
    org2.setOrganizationId(orgId2);
    org2.setOrgName("Comune di Test 2");
    org2.setStatus(OrganizationStatus.ACTIVE);
    org2.setBrokerId(brokerId);

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
    paymentOption.setPaymentOptionType(PaymentOptionTypeEnum.DOWN_PAYMENT);
    paymentOption.setInstallments(List.of(installment));

    DebtPositionDTO dp = new DebtPositionDTO();
    dp.setPaymentOptions(List.of(paymentOption));

    byte[] marshalledBytes = "dovuti-xml".getBytes();
    when(jaxbTransformServiceMock.marshallingAsBytes(any(Dovuti.class), eq(Dovuti.class)))
      .thenReturn(marshalledBytes);

    // When
    PaaSILChiediPosizioniAperteRisposta response;
    try (MockedStatic<AuthorizationService> mockedAuth = Mockito.mockStatic(AuthorizationService.class)) {
      mockedAuth.when(() -> AuthorizationService.validateBrokerAdminRole(eq(userInfo)))
        .thenAnswer(inv -> null);

      if (orgIpaCode != null) {
        // Single organization path
        mockedAuth.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode)))
          .thenReturn(orgId1);
        mockedAuth.when(() -> AuthorizationService.isOrganizationHandledByBroker(eq(brokerId), eq(userInfo)))
          .thenAnswer(inv -> null);

        when(organizationServiceMock.getOrganizationById(orgId1, accessToken))
          .thenReturn(Optional.of(org1));

        when(debtPositionServiceMock.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
          eq("RSSMRA80A01H501U"), any(PersonEntityType.class), eq(List.of(orgId1)), eq(debtPositionTypeOrgCodesToExclude), eq(InstallmentStatus.UNPAID), eq(dateFilter), eq(accessToken))
        ).thenReturn(List.of(dp));
      } else {
        // Broker organizations path
        when(organizationServiceMock.findByBrokerIdAndStatus(brokerId, OrganizationStatus.ACTIVE, accessToken))
          .thenReturn(List.of(org1, org2));

        when(debtPositionServiceMock.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
          eq("RSSMRA80A01H501U"), any(PersonEntityType.class), eq(List.of(orgId1, orgId2)), eq(debtPositionTypeOrgCodesToExclude), eq(InstallmentStatus.UNPAID), eq(dateFilter), eq(accessToken))
        ).thenReturn(List.of(dp));
      }

      response = service.processRequest(request, userInfo, accessToken);
    }

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getPaaSILPosizioniApertes());
    Assertions.assertEquals(expectedOrgCount, response.getPaaSILPosizioniApertes().size());

    PaaSILPosizioniAperte item = response.getPaaSILPosizioniApertes().getFirst();
    Assertions.assertEquals(orgIpaCode, item.getCodIpaEnte());
    Assertions.assertEquals("Comune di Test", item.getDeNomeEnte());
    DataHandler dh = item.getDovuti();
    Assertions.assertNotNull(dh, "Dovuti DataHandler should be set");

    // Verify interactions
    if (orgIpaCode != null) {
      verify(organizationServiceMock).getOrganizationById(orgId1, accessToken);
      verify(organizationServiceMock, never()).findByBrokerIdAndStatus(anyLong(), any(OrganizationStatus.class), anyString());
      verify(debtPositionServiceMock).getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
        eq("RSSMRA80A01H501U"), any(PersonEntityType.class), eq(List.of(orgId1)), eq(debtPositionTypeOrgCodesToExclude), eq(InstallmentStatus.UNPAID), eq(dateFilter), eq(accessToken)
      );
    } else {
      verify(organizationServiceMock, never()).getOrganizationById(anyLong(), anyString());
      verify(organizationServiceMock).findByBrokerIdAndStatus(brokerId, OrganizationStatus.ACTIVE, accessToken);
      verify(debtPositionServiceMock).getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
        eq("RSSMRA80A01H501U"), any(PersonEntityType.class), eq(List.of(orgId1, orgId2)), eq(debtPositionTypeOrgCodesToExclude), eq(InstallmentStatus.UNPAID), eq(dateFilter), eq(accessToken)
      );
    }

    verify(jaxbTransformServiceMock, times(expectedOrgCount)).marshallingAsBytes(any(Dovuti.class), eq(Dovuti.class));
  }
}
