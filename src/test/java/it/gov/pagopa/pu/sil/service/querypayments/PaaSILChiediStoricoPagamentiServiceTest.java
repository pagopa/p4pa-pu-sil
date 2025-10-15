package it.gov.pagopa.pu.sil.service.querypayments;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.debtpositions.ReceiptService;
import it.gov.pagopa.pu.sil.connector.fileshare.FileShareService;
import it.gov.pagopa.pu.sil.connector.organization.service.OrganizationService;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.AuthorizationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.veneto.regione.pagamenti.ente.PaaSILChiediStoricoPagamenti;
import it.veneto.regione.pagamenti.ente.PaaSILChiediStoricoPagamentiRisposta;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtIdentificativoUnivocoPersonaFG;
import it.veneto.regione.schemas._2012.pagamenti.ente.PagatiConRicevuta;
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
import org.springframework.core.io.Resource;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaaSILChiediStoricoPagamentiServiceTest {

  @Mock private DebtPositionService debtPositionServiceMock;
  @Mock private OrganizationService organizationServiceMock;
  @Mock private JAXBTransformService jaxbTransformServiceMock;
  @Mock private ReceiptService receiptServiceMock;
  @Mock private FileShareService fileShareServiceMock;

  private PaaSILChiediStoricoPagamentiService service;

  @BeforeEach
  void setUp() {
    service = new PaaSILChiediStoricoPagamentiService(
      debtPositionServiceMock,
      organizationServiceMock,
      jaxbTransformServiceMock,
      receiptServiceMock,
      fileShareServiceMock
    );
  }

  @Test
  void givenValidRequestWhenProcessRequestThenBuildsPaymentsHistory() throws Exception {
    // Given
    String accessToken = "token";
    String orgIpaCode = "IPA123";
    long orgId = 42L;

    PaaSILChiediStoricoPagamenti request = new PaaSILChiediStoricoPagamenti();
    request.setCodIpaEnte(orgIpaCode);

    CtIdentificativoUnivocoPersonaFG idFG = new CtIdentificativoUnivocoPersonaFG();
    idFG.setCodiceIdentificativoUnivoco("RSSMRA80A01H501U");
    idFG.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.F);
    request.setIdentificativoUnivocoPersonaFG(idFG);

    // explicitly set date range to ensure deterministic assertions
    XMLGregorianCalendar dataFrom = toXmlCal(ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
    XMLGregorianCalendar dataTo = toXmlCal(ZonedDateTime.of(2025, 1, 31, 23, 59, 59, 0, ZoneId.systemDefault()));
    request.setDataFrom(dataFrom);
    request.setDataTo(dataTo);

    UserInfo userInfo = new UserInfo();

    Organization org = new Organization();
    org.setOrganizationId(orgId);
    org.setOrgName("Comune di Test");
    org.setOrgFiscalCode("11111111111");
    org.setStatus(OrganizationStatus.ACTIVE);

    // build debt position with one payment option and one paid installment
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
    dp.setOrganizationId(orgId);
    dp.setPaymentOptions(List.of(paymentOption));

    when(organizationServiceMock.getOrganizationById(eq(orgId), eq(accessToken)))
      .thenReturn(Optional.of(org));

    when(debtPositionServiceMock.getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
      anyString(), any(PersonEntityType.class), eq(orgId), eq(InstallmentStatus.PAID), any(), any(), eq(accessToken))
    ).thenReturn(List.of(dp));

    // receipt data
    ReceiptDTO receipt = new ReceiptDTO();
    receipt.setIdPsp("PSP001");
    receipt.setPspCompanyName("PSP Test");
    receipt.setPaymentReceiptId("RCP-123");
    receipt.setPaymentDateTime(java.time.OffsetDateTime.parse("2025-01-15T10:15:30+01:00"));
    receipt.setPaymentNote("note");
    receipt.setCreditorReferenceId("IUV123");
    receipt.setPaymentAmountCents(12345L);
    receipt.setFeeCents(200L);

    when(receiptServiceMock.getReceiptById(eq(1001L), eq(accessToken))).thenReturn(receipt);

    // file share returns receipt bytes
    Resource resourceMock = mock(Resource.class);
    when(resourceMock.getContentAsByteArray()).thenReturn("pdf-binary".getBytes(StandardCharsets.UTF_8));
    when(fileShareServiceMock.downloadReceipt(eq(orgId), eq(1001L), eq(accessToken))).thenReturn(resourceMock);

    byte[] marshalledBytes = "pagati-con-ricevuta-xml".getBytes(StandardCharsets.UTF_8);
    when(jaxbTransformServiceMock.marshallingAsBytes(any(PagatiConRicevuta.class), eq(PagatiConRicevuta.class)))
      .thenReturn(marshalledBytes);

    // When
    PaaSILChiediStoricoPagamentiRisposta response;
    try (MockedStatic<AuthorizationService> mockedAuth = Mockito.mockStatic(AuthorizationService.class)) {
      mockedAuth.when(() -> AuthorizationService.validateAdminRole(eq(orgIpaCode), eq(userInfo)))
        .thenAnswer(inv -> null);
      mockedAuth.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode)))
        .thenReturn(orgId);

      response = service.processRequest(request, userInfo, accessToken, orgIpaCode);
    }

    // Then
    Assertions.assertNotNull(response);
    Assertions.assertEquals(dataTo, response.getDateTo());
    Assertions.assertNotNull(response.getPaaSILStoricoPagamentis());
    Assertions.assertEquals(1, response.getPaaSILStoricoPagamentis().size());

    var item = response.getPaaSILStoricoPagamentis().get(0);
    Assertions.assertEquals(orgIpaCode, item.getCodIpaEnte());
    Assertions.assertEquals("Comune di Test", item.getDeNomeEnte());
    DataHandler rtDh = item.getRt();
    DataHandler ctDh = item.getCtPagatiConRicevuta();
    Assertions.assertNotNull(rtDh, "RT DataHandler should be set");
    Assertions.assertNotNull(ctDh, "CtPagatiConRicevuta DataHandler should be set");

    verify(organizationServiceMock).getOrganizationById(orgId, accessToken);
    verify(debtPositionServiceMock).getDebtPositionsByDebtorFiscalCodeAndDebtorEntityType(
      eq("RSSMRA80A01H501U"), any(PersonEntityType.class), eq(orgId), eq(InstallmentStatus.PAID), any(), any(), eq(accessToken)
    );
    verify(receiptServiceMock).getReceiptById(1001L, accessToken);
    verify(fileShareServiceMock).downloadReceipt(orgId, 1001L, accessToken);
    verify(jaxbTransformServiceMock, times(1)).marshallingAsBytes(any(PagatiConRicevuta.class), eq(PagatiConRicevuta.class));
  }

  @Test
  void givenInactiveOrganizationWhenProcessRequestThenThrowsSilFaultException() throws Exception {
    // Given
    String accessToken = "token";
    String orgIpaCode = "IPA123";
    long orgId = 42L;

    PaaSILChiediStoricoPagamenti request = new PaaSILChiediStoricoPagamenti();
    request.setCodIpaEnte(orgIpaCode);

    CtIdentificativoUnivocoPersonaFG idFG = new CtIdentificativoUnivocoPersonaFG();
    idFG.setCodiceIdentificativoUnivoco("RSSMRA80A01H501U");
    idFG.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.F);
    request.setIdentificativoUnivocoPersonaFG(idFG);

    UserInfo userInfo = new UserInfo();

    when(organizationServiceMock.getOrganizationById(eq(orgId), eq(accessToken)))
      .thenReturn(Optional.empty());

    // When - Then
    try (MockedStatic<AuthorizationService> mockedAuth = Mockito.mockStatic(AuthorizationService.class)) {
      mockedAuth.when(() -> AuthorizationService.validateAdminRole(eq(orgIpaCode), eq(userInfo)))
        .thenAnswer(inv -> null);
      mockedAuth.when(() -> AuthorizationService.getOrganizationIdFromUserInfo(eq(userInfo), eq(orgIpaCode)))
        .thenReturn(orgId);

      Assertions.assertThrows(SilFaultException.class, () ->
        service.processRequest(request, userInfo, accessToken, orgIpaCode)
      );
    }

    verify(organizationServiceMock).getOrganizationById(orgId, accessToken);
    verifyNoInteractions(debtPositionServiceMock, receiptServiceMock, fileShareServiceMock, jaxbTransformServiceMock);
  }

  private static XMLGregorianCalendar toXmlCal(ZonedDateTime zdt) throws Exception {
    GregorianCalendar cal = GregorianCalendar.from(zdt);
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
  }
}
