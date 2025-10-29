package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionInstallmentService;
import it.gov.pagopa.pu.sil.service.immediatepayments.PaymentRequestMappingResult;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaaSILInviaDovutiMapperTest {

  @InjectMocks
  private PaaSILInviaDovutiMapper mapper;

  @Mock
  private JAXBTransformService jaxbTransformServiceMock;

  @Mock
  private DebtPositionTypeService debtPositionTypeServiceMock;

  @Mock
  private PersonMapper personMapperMock;

  @Mock
  private ValidationService validationServiceMock;

  @Mock
  private DebtPositionInstallmentService debtPositionInstallmentServiceMock;

  UserInfo userInfo;
  Organization org;

  private static final String ACCESS_TOKEN = "token";

  private static final String ORG_IPA_CODE = "ORG_IPA";

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setup() {
    userInfo = podamFactory.manufacturePojo(UserInfo.class);
    userInfo.getOrganizations().getFirst().setOrganizationIpaCode(ORG_IPA_CODE);
    userInfo.getOrganizations().getFirst().setRoles(List.of("ROLE_X", "ROLE_ADMIN"));
    org = podamFactory.manufacturePojo(Organization.class);
    org.setIpaCode(ORG_IPA_CODE);
    org.setStatus(OrganizationStatus.ACTIVE);
  }

  @Test
  void mapRequestToDebtPositions_UnmarshallingFailure_ReturnsError() {
    PaaSILInviaDovuti request = new PaaSILInviaDovuti();
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenThrow(new ApplicationException("Error"));

    SilFaultException exception = assertThrows(SilFaultException.class, () -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

    assertEquals(SilFaults.PAA_XML_NON_VALIDO, exception.getFault());
    assertTrue(exception.getDescription().contains("XML non conforme"));
  }

  @Test
  void mapRequestToDebtPositions_InvalidIUV_ReturnsError() {
    PaaSILInviaDovuti request = new PaaSILInviaDovuti();
    Dovuti dovuti = podamFactory.manufacturePojo(Dovuti.class);
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().clear();
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().add(new CtDatiSingoloVersamentoDovuti());
    dovuti.getDatiVersamento().setIdentificativoUnivocoVersamento("IUV");
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenReturn(dovuti);
    doNothing().when(validationServiceMock).validateCartSize(dovuti.getDatiVersamento().getDatiSingoloVersamentos().size());
    SilFaultException exception = assertThrows(SilFaultException.class, () -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

    assertEquals(SilFaults.PAA_IUV_NON_VALIDO, exception.getFault());
    assertEquals("L'inserimento dello IUV è deprecato", exception.getDescription());

  }

  @Test
  void mapRequestToDebtPositions_InvalidDebtor_ReturnsError() {
    PaaSILInviaDovuti request = new PaaSILInviaDovuti();
    Dovuti dovuti = podamFactory.manufacturePojo(Dovuti.class);
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().clear();
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().add(new CtDatiSingoloVersamentoDovuti());
    dovuti.getDatiVersamento().setIdentificativoUnivocoVersamento(null);
    dovuti.setSoggettoPagatore(new CtSoggettoPagatore());
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenReturn(dovuti);
    doThrow(new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "error")).when(personMapperMock).getAndValidateDebtor(any());
    doNothing().when(validationServiceMock).validateCartSize(dovuti.getDatiVersamento().getDatiSingoloVersamentos().size());
    SilFaultException exception = assertThrows(SilFaultException.class, () -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("error", exception.getDescription());
  }

  @ParameterizedTest
  @ValueSource(strings = {"empty", "6dp"})
  void mapRequestToDebtPositions_InvalidNumberOfDebtPositions_ReturnsError(String testCase) {
    PaaSILInviaDovuti request = new PaaSILInviaDovuti();
    Dovuti dovuti = podamFactory.manufacturePojo(Dovuti.class);
    dovuti.getDatiVersamento().setIdentificativoUnivocoVersamento(null);
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().clear();
    SilFaultException silFaultException = new SilFaultException(SilFaults.PAA_XML_NON_VALIDO, "Nessun dovuto presente");
    if(testCase.equals("6dp")) {
      for(int i = 0; i < 6; i++) {
        CtDatiSingoloVersamentoDovuti versamento = podamFactory.manufacturePojo(CtDatiSingoloVersamentoDovuti.class);
        dovuti.getDatiVersamento().getDatiSingoloVersamentos().add(versamento);
      }
      silFaultException = new SilFaultException(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_CARRELLO, "Numero massimo dovuti nel carrello superato: 6/5");
    }
    doThrow(silFaultException).when(validationServiceMock)
      .validateCartSize(dovuti.getDatiVersamento().getDatiSingoloVersamentos().size());
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenReturn(dovuti);

    SilFaultException exception = assertThrows(SilFaultException.class, () -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

    if(testCase.equals("6dp")){
      assertEquals(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_CARRELLO, exception.getFault());
      assertEquals("Numero massimo dovuti nel carrello superato: 6/5", exception.getDescription());
    } else {
      assertEquals(SilFaults.PAA_XML_NON_VALIDO, exception.getFault());
      assertEquals("Nessun dovuto presente", exception.getDescription());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"stamp", "customValidCategory", "customInvalidCategory"})
  void mapRequestToDebtPositionsOrFault_ValidFlow_ReturnsDebtPositions(String testCase) {
    //given
    PaaSILInviaDovuti request = new PaaSILInviaDovuti();
    Dovuti dovuti = podamFactory.manufacturePojo(Dovuti.class);
    dovuti.setSoggettoPagatore(new CtSoggettoPagatore());
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    versamento.setCausaleVersamento("causale");
    versamento.setImportoSingoloVersamento(BigDecimal.TWO);
    String datiSpecificiRiscossione = "9/DATI_SPECIFICI";
    versamento.setIdentificativoTipoDovuto("COD_TIPO_DOVUTO");
    versamento.setIdentificativoUnivocoDovuto("IUD");
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().clear();
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().add(versamento);
    dovuti.getDatiVersamento().setIdentificativoUnivocoVersamento(null);
    DebtPositionTypeOrg debtPositionTypeOrg = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);
    DebtPositionType debtPositionType = podamFactory.manufacturePojo(DebtPositionType.class);
    String expectedCategory = debtPositionType.getTaxonomyCode();
    if (testCase.equals("stamp")) {
      CtDatiMarcaBolloDigitale datiMarcaBolloDigitale = new CtDatiMarcaBolloDigitale();
      datiMarcaBolloDigitale.setTipoBollo("01");
      datiMarcaBolloDigitale.setHashDocumento("HASH_DOCUMENTO");
      datiMarcaBolloDigitale.setProvinciaResidenza("IT");
      versamento.setDatiMarcaBolloDigitale(datiMarcaBolloDigitale);
    } else if(testCase.equals("customValidCategory")) {
      datiSpecificiRiscossione = "9/1234567IM/CUSTOM_VALID_CATEGORY";
      expectedCategory = "9/1234567IM/";
    } else if(testCase.equals("customInvalidCategory")) {
      datiSpecificiRiscossione = "9/1234888/CUSTOM_INVALID_CATEGORY";
      debtPositionTypeOrg.setIban(null);
      debtPositionTypeOrg.setPostalIban(null);
      org.setIban("IT60X0542811101000000123456");
      org.setPostalIban("IT60X0542811101000000654321");
    }
    versamento.setDatiSpecificiRiscossione(datiSpecificiRiscossione);
    PersonDTO debtor = podamFactory.manufacturePojo(PersonDTO.class);
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenReturn(dovuti);
    doNothing().when(validationServiceMock).validateCartSize(dovuti.getDatiVersamento().getDatiSingoloVersamentos().size());
    when(personMapperMock.getAndValidateDebtor(any())).thenReturn(debtor);
    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(
      org.getOrganizationId(), versamento.getIdentificativoTipoDovuto(), ACCESS_TOKEN)).thenReturn(debtPositionTypeOrg);
    when(debtPositionInstallmentServiceMock.getCategory(datiSpecificiRiscossione, versamento.getIdentificativoTipoDovuto(), org.getOrganizationId(), ACCESS_TOKEN)).thenReturn(expectedCategory);


    //when
    PaymentRequestMappingResult paymentRequestMappingResult = mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN);
    List<DebtPositionDTO> result = paymentRequestMappingResult.debtPositions();

    //then
    assertNotNull(result);
    assertEquals(1, result.size());

    result.forEach(dp -> {
      TestUtils.checkAllNotNullFields(dp,
        "debtPositionId", "validityDate", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
      dp.getPaymentOptions().forEach(po -> {
        TestUtils.checkNotNullFields(po, "paymentOptionId", "debtPositionId", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId", "description");
        po.getInstallments().forEach(i -> {
          TestUtils.checkAllNotNullFields(i, "installmentId", "paymentOptionId", "syncStatus", "iupdPagopa", "generateNotice",
            "iuv", "iur", "iuf", "nav", "iun", "notificationFeeCents", "transfers", "notificationDate", "ingestionFlowFileId",
            "ingestionFlowFileAction", "ingestionFlowFileLineNumber", "receiptId", "switchToExpired",
            "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId", "balance");
            assertNotNull(i.getTransfers());
            assertEquals(1, i.getTransfers().size());
            TransferDTO transfer = i.getTransfers().getFirst();
            String excludedFields = String.join(",", "transferId", "installmentId",
              "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId", "mbdAttachment");
            if(testCase.equals("stamp")){
              excludedFields += ",iban,postalIban";
              assertNull(transfer.getIban());
              assertNull(transfer.getPostalIban());
            } else {
              excludedFields += ",stampHashDocument,stampProvincialResidence,stampType";
              assertNull(transfer.getStampHashDocument());
              assertNull(transfer.getStampType());
              assertNull(transfer.getStampProvincialResidence());
            }
            TestUtils.checkAllNotNullFields(transfer, excludedFields.split(","));
        });
      });
    });
  }

  @Test
  void whenMapRequestToDebtPositionsThenReturnMixedDebtPosition() {
    // given
    PaaSILInviaDovuti request = new PaaSILInviaDovuti();
    Dovuti dovuti = podamFactory.manufacturePojo(Dovuti.class);
    dovuti.setSoggettoPagatore(new CtSoggettoPagatore());

    CtDatiMarcaBolloDigitale datiMarcaBolloDigitale = new CtDatiMarcaBolloDigitale();
    datiMarcaBolloDigitale.setTipoBollo("01");
    datiMarcaBolloDigitale.setHashDocumento("HASH_DOCUMENTO");
    datiMarcaBolloDigitale.setProvinciaResidenza("IT");
    CtDatiSingoloVersamentoDovuti stampTransfer = new CtDatiSingoloVersamentoDovuti();
    stampTransfer.setCausaleVersamento("causale STAMP");
    stampTransfer.setImportoSingoloVersamento(BigDecimal.TEN);
    stampTransfer.setIdentificativoTipoDovuto("STAMP");
    stampTransfer.setIdentificativoUnivocoDovuto("IUD-STAMP");
    stampTransfer.setDatiMarcaBolloDigitale(datiMarcaBolloDigitale);

    CtDatiSingoloVersamentoDovuti noIbanTransfer = new CtDatiSingoloVersamentoDovuti();
    noIbanTransfer.setCausaleVersamento("causale NO-IBAN");
    noIbanTransfer.setImportoSingoloVersamento(BigDecimal.TWO);
    noIbanTransfer.setIdentificativoTipoDovuto("NO-IBAN");
    noIbanTransfer.setIdentificativoUnivocoDovuto("IUD-NO-IBAN");
    noIbanTransfer.setDatiSpecificiRiscossione("9/1234567IM/");

    CtDatiSingoloVersamentoDovuti balanceTransfer = new CtDatiSingoloVersamentoDovuti();
    balanceTransfer.setCausaleVersamento("causale BALANCE");
    balanceTransfer.setImportoSingoloVersamento(BigDecimal.ONE);
    balanceTransfer.setIdentificativoTipoDovuto("BALANCE");
    balanceTransfer.setIdentificativoUnivocoDovuto("IUD-BALANCE");
    balanceTransfer.setDatiSpecificiRiscossione("9/1234567IM/");
    Bilancio bilancio = podamFactory.manufacturePojo(Bilancio.class);
    balanceTransfer.setBilancio(bilancio);
    when(jaxbTransformServiceMock.marshalling(bilancio, Bilancio.class)).thenReturn("bilancioString");

    dovuti.getDatiVersamento().setIdentificativoUnivocoVersamento(null);
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().clear();
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().add(stampTransfer);
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().add(noIbanTransfer);
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().add(balanceTransfer);


    DebtPositionTypeOrg debtPositionTypeOrgStamp = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);
    DebtPositionTypeOrg debtPositionTypeOrgBalance = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);
    DebtPositionTypeOrg debtPositionTypeOrgNoIban = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);
    debtPositionTypeOrgNoIban.setIban(null);
    debtPositionTypeOrgNoIban.setPostalIban(null);


    PersonDTO debtor = podamFactory.manufacturePojo(PersonDTO.class);
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenReturn(dovuti);
    doNothing().when(validationServiceMock).validateCartSize(dovuti.getDatiVersamento().getDatiSingoloVersamentos().size());
    when(personMapperMock.getAndValidateDebtor(any())).thenReturn(debtor);

    doReturn(debtPositionTypeOrgStamp).when(debtPositionTypeServiceMock)
        .getDebtPositionTypeOrgByOrgIdAndType(org.getOrganizationId(), stampTransfer.getIdentificativoTipoDovuto(), ACCESS_TOKEN);
    doReturn(debtPositionTypeOrgBalance).when(debtPositionTypeServiceMock)
      .getDebtPositionTypeOrgByOrgIdAndType(org.getOrganizationId(), balanceTransfer.getIdentificativoTipoDovuto(), ACCESS_TOKEN);
    doReturn(debtPositionTypeOrgNoIban).when(debtPositionTypeServiceMock)
      .getDebtPositionTypeOrgByOrgIdAndType(org.getOrganizationId(), noIbanTransfer.getIdentificativoTipoDovuto(), ACCESS_TOKEN);

    //when
    PaymentRequestMappingResult paymentRequestMappingResult = mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN);
    List<MixedDebtPositionDTO> result = paymentRequestMappingResult.mixedDebtPositions();

    //then
    assertNotNull(result);
    assertEquals(1, result.size());
    MixedDebtPositionDTO mixedDebtPositionDTO = result.getFirst();
    TestUtils.checkAllNotNullFields(mixedDebtPositionDTO);

    List<MixedTransferDTO> mixedTransfers = mixedDebtPositionDTO.getTransfers();
    assertEquals(3, mixedTransfers.size());
    mixedTransfers.forEach(mt -> {
      String excludedFields = "balance";
      if(mt.getIud().contains("STAMP")) {
        excludedFields += ",iban,postalIban,legacyPaymentMetadata";
        assertNull(mt.getIban());
        assertNull(mt.getPostalIban());
        assertNull(mt.getLegacyPaymentMetadata());
      } else {
        excludedFields += ",stampHashDocument,stampProvincialResidence,stampType";
        assertNull(mt.getStampHashDocument());
        assertNull(mt.getStampType());
        assertNull(mt.getStampProvincialResidence());
      }
      TestUtils.checkAllNotNullFields(mt, excludedFields.split(","));
    });
  }
}

