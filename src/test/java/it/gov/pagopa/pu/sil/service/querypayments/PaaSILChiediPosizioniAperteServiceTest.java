package it.gov.pagopa.pu.sil.service.querypayments;

import static it.gov.pagopa.pu.sil.util.Constants.EXCLUDED_DEBT_POSITION_TYPE_CODES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.mapper.DovutiMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionCheckoutService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPosizioniAperte;
import it.veneto.regione.pagamenti.ente.PaaSILChiediPosizioniAperteRisposta;
import it.veneto.regione.pagamenti.ente.PaaSILPosizioniAperte;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtIdentificativoUnivocoPersonaFG;
import it.veneto.regione.schemas._2012.pagamenti.ente.Dovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.StTipoIdentificativoUnivocoPersFG;
import jakarta.activation.DataHandler;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

@ExtendWith(MockitoExtension.class)
class PaaSILChiediPosizioniAperteServiceTest {

  @Mock private DebtPositionService debtPositionServiceMock;
  @Mock private OrganizationService organizationServiceMock;
  @Mock private AuthorizationService authorizationServiceMock;
  @Mock private JAXBTransformService jaxbTransformServiceMock;
  @Mock private DebtPositionTypeService debtPositionTypeServiceMock;
  @Mock private DebtPositionCheckoutService debtPositionCheckoutServiceMock;
  @Mock private DovutiMapper dovutiMapperMock;

  private PaaSILChiediPosizioniAperteService service;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    service = new PaaSILChiediPosizioniAperteService(
      "http://test-url",
      debtPositionServiceMock,
      organizationServiceMock,
      authorizationServiceMock,
      jaxbTransformServiceMock,
      debtPositionCheckoutServiceMock,
      debtPositionTypeServiceMock,
      dovutiMapperMock
    );
  }

  @ParameterizedTest
  @ValueSource(booleans =  {true, false})
  void givenValidRequestWhenProcessRequestThenBuildsOpenPositions(boolean useIpaCode) {
    // Given
    String orgIpaCode = useIpaCode ? "IPA123" : null;
    String accessToken = "token";
    long orgId = 42L;
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

    Organization org = new Organization();
    org.setOrganizationId(orgId);
    org.setOrgName("Comune di Test");
    org.setStatus(OrganizationStatus.ACTIVE);
    org.setBrokerId(brokerId);
    Optional.ofNullable(orgIpaCode).ifPresent(org::setIpaCode);

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
    installment.setStatus(InstallmentStatus.UNPAID);

    PaymentOptionDTO paymentOption = new PaymentOptionDTO();
    paymentOption.setPaymentOptionType(PaymentOptionType.DOWN_PAYMENT);
    paymentOption.setInstallments(List.of(installment));

    DebtPositionDTO dp = new DebtPositionDTO();
    dp.setOrganizationId(orgId);
    dp.setPaymentOptions(List.of(paymentOption));

    DebtPositionTypeOrg debtPositionTypeOrg = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);

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
          .thenReturn(orgId);
        mockedAuth.when(() -> AuthorizationService.isOrganizationHandledByBroker(eq(brokerId), eq(userInfo)))
          .thenAnswer(inv -> null);

        when(organizationServiceMock.getOrganizationById(orgId, accessToken))
          .thenReturn(Optional.of(org));

      } else {
        // Broker organizations path
        when(organizationServiceMock.findByBrokerIdAndStatus(brokerId, OrganizationStatus.ACTIVE, accessToken))
          .thenReturn(List.of(org));

      }
      when(debtPositionServiceMock.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
        eq("RSSMRA80A01H501U"), any(PersonEntityType.class), eq(List.of(orgId)), eq(debtPositionTypeOrgCodesToExclude), eq(InstallmentStatus.UNPAID), eq(dateFilter), eq(accessToken))
      ).thenReturn(List.of(dp));

      when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByInstallmentId(installment.getInstallmentId(), accessToken))
        .thenReturn(debtPositionTypeOrg);
      when(dovutiMapperMock.map(installment,debtPositionTypeOrg))
        .thenReturn(podamFactory.manufacturePojo(Dovuti.class));

      response = service.processRequest(request, userInfo, accessToken);
    }

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response.getPaaSILPosizioniApertes());
    Assertions.assertEquals(1, response.getPaaSILPosizioniApertes().size());

    PaaSILPosizioniAperte item = response.getPaaSILPosizioniApertes().getFirst();
    Assertions.assertEquals(orgIpaCode, item.getCodIpaEnte());
    Assertions.assertEquals("Comune di Test", item.getDeNomeEnte());
    DataHandler dh = item.getDovuti();
    Assertions.assertNotNull(dh, "Dovuti DataHandler should be set");

    // Verify interactions
    if (orgIpaCode != null) {
      verify(organizationServiceMock).getOrganizationById(orgId, accessToken);
      verify(organizationServiceMock, never()).findByBrokerIdAndStatus(anyLong(), any(OrganizationStatus.class), anyString());
    } else {
      verify(organizationServiceMock, never()).getOrganizationById(anyLong(), anyString());
      verify(organizationServiceMock).findByBrokerIdAndStatus(brokerId, OrganizationStatus.ACTIVE, accessToken);
    }
    verify(debtPositionServiceMock).getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
      eq("RSSMRA80A01H501U"), any(PersonEntityType.class), eq(List.of(orgId)), eq(debtPositionTypeOrgCodesToExclude), eq(InstallmentStatus.UNPAID), eq(dateFilter), eq(accessToken)
    );

    verify(jaxbTransformServiceMock, times(1)).marshallingAsBytes(any(Dovuti.class), eq(Dovuti.class));
  }
}
