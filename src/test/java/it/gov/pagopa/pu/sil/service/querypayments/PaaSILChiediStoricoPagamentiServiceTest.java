package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.PagatiMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.receipt.ReceiptService;
import it.veneto.regione.pagamenti.ente.PaaSILChiediStoricoPagamenti;
import it.veneto.regione.pagamenti.ente.PaaSILChiediStoricoPagamentiRisposta;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtIdentificativoUnivocoPersonaFG;
import it.veneto.regione.schemas._2012.pagamenti.ente.StTipoIdentificativoUnivocoPersFG;
import jakarta.activation.DataHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.pu.sil.util.Constants.EXCLUDED_DEBT_POSITION_TYPE_CODES;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaaSILChiediStoricoPagamentiServiceTest {

  @Mock private DebtPositionService debtPositionServiceMock;
  @Mock private OrganizationService organizationServiceMock;
  @Mock private ReceiptService receiptServiceMock;
  @Mock private PagatiMapper pagatiMapperMock;

  private PaaSILChiediStoricoPagamentiService service;

  @BeforeEach
  void setUp() {
    service = new PaaSILChiediStoricoPagamentiService(
      debtPositionServiceMock,
      organizationServiceMock,
      receiptServiceMock,
      pagatiMapperMock
    );
  }

  @ParameterizedTest
  @CsvSource(value = {
    "IPA123, 1",           // With orgIpaCode - single organization path
    "null, 2"              // Without orgIpaCode - broker organizations path (multiple orgs)
  }, nullValues = {"null"})
  void givenValidRequestWhenProcessRequestThenBuildsPaymentsHistory(String orgIpaCode, int expectedOrgCount) throws Exception {
    // Given
    String accessToken = "token";
    long orgId1 = 42L;
    long orgId2 = 43L;
    Long brokerId = 100L;
    List<String> debtPositionTypeOrgCodesToExclude = EXCLUDED_DEBT_POSITION_TYPE_CODES;

    PaaSILChiediStoricoPagamenti request = new PaaSILChiediStoricoPagamenti();
    request.setCodIpaEnte(orgIpaCode);

    CtIdentificativoUnivocoPersonaFG idFG = new CtIdentificativoUnivocoPersonaFG();
    idFG.setCodiceIdentificativoUnivoco("RSSMRA80A01H501U");
    idFG.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.F);
    request.setIdentificativoUnivocoPersonaFG(idFG);

    XMLGregorianCalendar dataFrom = toXmlCal(ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
    XMLGregorianCalendar dataTo = toXmlCal(ZonedDateTime.of(2025, 1, 31, 23, 59, 59, 0, ZoneId.systemDefault()));
    request.setDataFrom(dataFrom);
    request.setDataTo(dataTo);

    UserInfo userInfo = new UserInfo();
    userInfo.setBrokerId(brokerId);

    Organization org1 = new Organization();
    org1.setOrganizationId(orgId1);
    org1.setOrgName("Comune di Test");
    org1.setOrgFiscalCode("11111111111");
    org1.setStatus(OrganizationStatus.ACTIVE);
    org1.setBrokerId(brokerId);

    Organization org2 = new Organization();
    org2.setOrganizationId(orgId2);
    org2.setOrgName("Comune di Test 2");
    org2.setOrgFiscalCode("22222222222");
    org2.setStatus(OrganizationStatus.ACTIVE);
    org2.setBrokerId(brokerId);

    InstallmentDTO installment = new InstallmentDTO();
    installment.setReceiptId(1001L);

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
    installment.setIud("IUD-001");
    installment.setLegacyPaymentMetadata("MBDAAA|MBDBBB");

    TransferDTO transfer = new TransferDTO();
    transfer.setTransferIndex(1);
    transfer.setAmountCents(12345L);
    transfer.setRemittanceInformation("Causale versamento");
    transfer.setMbdAttachment("allegato-mbd");
    installment.setTransfers(List.of(transfer));

    PaymentOptionDTO paymentOption = new PaymentOptionDTO();
    paymentOption.setInstallments(List.of(installment));

    DebtPositionDTO dp = new DebtPositionDTO();
    dp.setOrganizationId(orgId1);
    dp.setPaymentOptions(List.of(paymentOption));

    byte[] marshalledReceipt = "receipt".getBytes(StandardCharsets.UTF_8);
    byte[] pagatiBytes = "pagati".getBytes(StandardCharsets.UTF_8);

    when(receiptServiceMock.getReceiptById(1001L, orgId1, accessToken)).thenReturn(marshalledReceipt);
    when(pagatiMapperMock.mapDebtPositionsToEncodedPagatiConRicevuta(installment, org1, accessToken))
      .thenReturn(pagatiBytes);

    // When
    PaaSILChiediStoricoPagamentiRisposta response;
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
          eq("RSSMRA80A01H501U"), any(PersonEntityType.class), eq(List.of(orgId1)), eq(debtPositionTypeOrgCodesToExclude), eq(InstallmentStatus.PAID), any(), eq(accessToken))
        ).thenReturn(List.of(dp));
      } else {
        // Broker organizations path
        when(organizationServiceMock.findByBrokerIdAndStatus(brokerId, OrganizationStatus.ACTIVE, accessToken))
          .thenReturn(List.of(org1, org2));

        when(debtPositionServiceMock.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
          eq("RSSMRA80A01H501U"), any(PersonEntityType.class), eq(List.of(orgId1, orgId2)), eq(debtPositionTypeOrgCodesToExclude), eq(InstallmentStatus.PAID), any(), eq(accessToken))
        ).thenReturn(List.of(dp));
      }

      response = service.processRequest(request, userInfo, accessToken);
    }

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(dataTo, response.getDateTo());
    Assertions.assertNotNull(response.getPaaSILStoricoPagamentis());
    Assertions.assertEquals(expectedOrgCount, response.getPaaSILStoricoPagamentis().size());

    var item = response.getPaaSILStoricoPagamentis().getFirst();
    Assertions.assertEquals(orgIpaCode, item.getCodIpaEnte());
    Assertions.assertEquals("Comune di Test", item.getDeNomeEnte());
    DataHandler rtDh = item.getRt();
    DataHandler ctDh = item.getCtPagatiConRicevuta();
    Assertions.assertNotNull(rtDh, "RT DataHandler should be set");
    Assertions.assertNotNull(ctDh, "CtPagatiConRicevuta DataHandler should be set");

    // Verify interactions
    if (orgIpaCode != null) {
      verify(organizationServiceMock).getOrganizationById(orgId1, accessToken);
      verify(organizationServiceMock, never()).findByBrokerIdAndStatus(anyLong(), any(OrganizationStatus.class), anyString());
      verify(debtPositionServiceMock).getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
        eq("RSSMRA80A01H501U"), any(PersonEntityType.class), eq(List.of(orgId1)), eq(debtPositionTypeOrgCodesToExclude), eq(InstallmentStatus.PAID), any(), eq(accessToken)
      );
    } else {
      verify(organizationServiceMock, never()).getOrganizationById(anyLong(), anyString());
      verify(organizationServiceMock).findByBrokerIdAndStatus(brokerId, OrganizationStatus.ACTIVE, accessToken);
      verify(debtPositionServiceMock).getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
        eq("RSSMRA80A01H501U"), any(PersonEntityType.class), eq(List.of(orgId1, orgId2)), eq(debtPositionTypeOrgCodesToExclude), eq(InstallmentStatus.PAID), any(), eq(accessToken)
      );
    }
  }

  @Test
  void givenInactiveOrganizationWhenProcessRequestThenThrowsSilFaultException() {
    // Given
    String accessToken = "token";
    String orgIpaCode = "IPA123";
    long orgId = 42L;
    Long brokerId = 100L;

    PaaSILChiediStoricoPagamenti request = new PaaSILChiediStoricoPagamenti();
    request.setCodIpaEnte(orgIpaCode);

    CtIdentificativoUnivocoPersonaFG idFG = new CtIdentificativoUnivocoPersonaFG();
    idFG.setCodiceIdentificativoUnivoco("RSSMRA80A01H501U");
    idFG.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.F);
    request.setIdentificativoUnivocoPersonaFG(idFG);

    UserInfo userInfo = new UserInfo();
    userInfo.setBrokerId(brokerId);

    when(organizationServiceMock.getOrganizationById(orgId, accessToken))
      .thenReturn(Optional.empty());

    // When - Then
    try (MockedStatic<AuthorizationService> mockedAuth = Mockito.mockStatic(AuthorizationService.class)) {
      mockedAuth.when(() -> AuthorizationService.validateBrokerAdminRole(eq(userInfo)))
        .thenAnswer(inv -> null);
      mockedAuth.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode)))
        .thenReturn(orgId);

      Assertions.assertThrows(SilFaultException.class, () ->
        service.processRequest(request, userInfo, accessToken)
      );
    }

    verify(organizationServiceMock).getOrganizationById(orgId, accessToken);
    verifyNoInteractions(debtPositionServiceMock, receiptServiceMock, receiptServiceMock, pagatiMapperMock);
  }

  private static XMLGregorianCalendar toXmlCal(ZonedDateTime zdt) throws Exception {
    GregorianCalendar cal = GregorianCalendar.from(zdt);
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
  }
}
