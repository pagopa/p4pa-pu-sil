package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.immediatepayments.PaymentRequestMappingResult;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.*;
import it.veneto.regione.schemas._2012.pagamenti.ente.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import uk.co.jemos.podam.api.PodamFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaaSILInviaCarrelloDovutiMapperTest {

  @InjectMocks
  private PaaSILInviaCarrelloDovutiMapper mapper;

  @Mock
  private JAXBTransformService jaxbTransformServiceMock;

  @Mock
  private DebtPositionTypeService debtPositionTypeServiceMock;

  @Mock
  private PersonMapper personMapperMock;

  @Mock
  private ValidationService validationServiceMock;

  @Mock
  private SecondaryTransferMapper secondaryTransferMapperMock;

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
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    request.setListaDovuti(new ListaDovuti());
    request.getListaDovuti().getElementoListaDovutis().add(new ElementoListaDovuti());
    request.getListaDovuti().getElementoListaDovutis().getFirst().setDovuti(new byte[]{});
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenThrow(new ApplicationException("Error"));

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

    assertEquals(SilFaults.PAA_XML_NON_VALIDO, exception.getFault());
    assertTrue(exception.getDescription().contains("XML dovuti [1] non conforme"));
  }

  @Test
  void mapRequestToDebtPositions_InvalidPrimaryEnte_ReturnsError() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    doThrow(new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "error")).when(validationServiceMock).validatePrimaryDebtPositionOrganization(request, ORG_IPA_CODE);

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

    assertEquals(SilFaults.PAA_ENTE_NON_VALIDO, exception.getFault());
    assertEquals("error", exception.getDescription());
  }

  @Test
  void mapRequestToDebtPositions_InvalidSecondaryEnte_ReturnsError() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    request.setListaDovuti(new ListaDovuti());
    request.getListaDovuti().getElementoListaDovutis().add(new ElementoListaDovuti());
    request.getListaDovuti().getElementoListaDovutis().getFirst().setDovuti(new byte[]{});
    doThrow(new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "error")).when(validationServiceMock).validateSecondaryDebtPositionCount(request, 1);

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

    assertEquals(SilFaults.PAA_ENTE_NON_VALIDO, exception.getFault());
    assertEquals("error", exception.getDescription());
  }

  @Test
  void mapRequestToDebtPositions_maxCartSize_ReturnsError() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    request.setListaDovuti(new ListaDovuti());
    for (int i = 0; i < 8; i++) {
      ElementoListaDovuti elem = new ElementoListaDovuti();
      elem.setDovuti(new byte[]{});
      request.getListaDovuti().getElementoListaDovutis().add(elem);
    }
    doThrow(new SilFaultException(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_CARRELLO, "Numero massimo dovuti nel carrello superato: 8/5"))
      .when(validationServiceMock).validateCartSize(request.getListaDovuti().getElementoListaDovutis().size());
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

    assertEquals(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_CARRELLO, exception.getFault());
    assertEquals("Numero massimo dovuti nel carrello superato: 8/5", exception.getDescription());
  }

  @Test
  void mapRequestToDebtPositions_InvalidIUV_ReturnsError() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    request.setListaDovuti(new ListaDovuti());
    request.getListaDovuti().getElementoListaDovutis().add(new ElementoListaDovuti());
    request.getListaDovuti().getElementoListaDovutis().getFirst().setDovuti(new byte[]{});
    Dovuti dovuti = podamFactory.manufacturePojo(Dovuti.class);
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().clear();
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().add(new CtDatiSingoloVersamentoDovuti());
    dovuti.getDatiVersamento().setIdentificativoUnivocoVersamento("IUV");
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenReturn(dovuti);
    doNothing().when(validationServiceMock).validateCartSize(request.getListaDovuti().getElementoListaDovutis().size());

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

    assertEquals(SilFaults.PAA_IUV_NON_VALIDO, exception.getFault());
    assertEquals("L'inserimento dello IUV è deprecato", exception.getDescription());
  }

  @Test
  void mapRequestToDebtPositions_InvalidDebtor_ReturnsError() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    request.setListaDovuti(new ListaDovuti());
    request.getListaDovuti().getElementoListaDovutis().add(new ElementoListaDovuti());
    request.getListaDovuti().getElementoListaDovutis().getFirst().setDovuti(new byte[]{});
    Dovuti dovuti = podamFactory.manufacturePojo(Dovuti.class);
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().clear();
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().add(new CtDatiSingoloVersamentoDovuti());
    dovuti.getDatiVersamento().setIdentificativoUnivocoVersamento(null);
    dovuti.setSoggettoPagatore(new CtSoggettoPagatore());
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenReturn(dovuti);
    doNothing().when(validationServiceMock).validateCartSize(request.getListaDovuti().getElementoListaDovutis().size());
    doThrow(new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "error")).when(personMapperMock).getAndValidateDebtor(any());

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class, () -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

    assertEquals(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, exception.getFault());
    assertEquals("error", exception.getDescription());
  }

  @ParameterizedTest
  @ValueSource(strings = {"9/1234567IM/"})
  @NullSource
  void mapRequestToDebtPositionsOrFault_ValidFlow_ReturnsDebtPositions(String legacyPaymentMetadataSecondary) {
    //given
    DebtPositionTypeOrg debtPositionTypeOrg = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);
    DebtPositionType debtPositionType = podamFactory.manufacturePojo(DebtPositionType.class);
    debtPositionTypeOrg.setDebtPositionTypeId(Objects.requireNonNull(debtPositionType.getDebtPositionTypeId()));

    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    request.setListaDovuti(new ListaDovuti());
    request.getListaDovuti().getElementoListaDovutis().add(new ElementoListaDovuti());
    request.getListaDovuti().getElementoListaDovutis().getFirst().setDovuti(new byte[]{});
    Dovuti dovuti = podamFactory.manufacturePojo(Dovuti.class);
    dovuti.setSoggettoPagatore(new CtSoggettoPagatore());
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    versamento.setCausaleVersamento("causale");
    versamento.setImportoSingoloVersamento(BigDecimal.TWO);
    versamento.setDatiSpecificiRiscossione("9/DATI_SPECIFICI");
    versamento.setIdentificativoTipoDovuto("COD_TIPO_DOVUTO");
    versamento.setIdentificativoUnivocoDovuto("IUD");
    Bilancio bilancio = podamFactory.manufacturePojo(Bilancio.class);
    versamento.setBilancio(bilancio);
    when(jaxbTransformServiceMock.marshalling(bilancio, Bilancio.class)).thenReturn("bilancioString");
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().clear();
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().add(versamento);
    dovuti.getDatiVersamento().setIdentificativoUnivocoVersamento(null);
    DovutiEntiSecondari dovutiEntiSecondari = podamFactory.manufacturePojo(DovutiEntiSecondari.class);
    dovutiEntiSecondari.getDatiVersamentoEntiSecondari().setImportoSingoloVersamento(BigDecimal.TEN);
    dovutiEntiSecondari.getDatiVersamentoEntiSecondari().setIbanAccreditoBeneficiario("IT60X0542811101000000123456");
    dovutiEntiSecondari.getDatiVersamentoEntiSecondari().setDatiSpecificiRiscossione(legacyPaymentMetadataSecondary);
    dovutiEntiSecondari.getDatiVersamentoEntiSecondari().setCodiceFiscaleBeneficiario("12345678901");
    request.setListaDovutiEntiSecondari(new ListaDovutiEntiSecondari());
    request.getListaDovutiEntiSecondari().getElementoListaDovutiEntiSecondaris().add(new ElementoListaDovutiEntiSecondari());
    request.getListaDovutiEntiSecondari().getElementoListaDovutiEntiSecondaris().getFirst().setDovutiEntiSecondari(new byte[]{});
    PersonDTO debtor = podamFactory.manufacturePojo(PersonDTO.class);
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenReturn(dovuti);
    doNothing().when(validationServiceMock).validateCartSize(request.getListaDovuti().getElementoListaDovutis().size());
    when(personMapperMock.getAndValidateDebtor(any())).thenReturn(debtor);
    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(
      org.getOrganizationId(), versamento.getIdentificativoTipoDovuto(), ACCESS_TOKEN)).thenReturn(debtPositionTypeOrg);
    CtDatiVersamentoDovutiEntiSecondari ctDatiVersamentoDovutiEntiSecondari = new CtDatiVersamentoDovutiEntiSecondari();
    String expectedCategory = legacyPaymentMetadataSecondary == null ? debtPositionType.getTaxonomyCode() : legacyPaymentMetadataSecondary.substring(2, 11);
    when(secondaryTransferMapperMock.mapToCtDatiVersamentoDovutiEntiSecondari(request.getListaDovutiEntiSecondari())).thenReturn(Optional.of(ctDatiVersamentoDovutiEntiSecondari));
    doNothing().when(validationServiceMock).validateSecondaryDebtPositionData(ctDatiVersamentoDovutiEntiSecondari, 1);
    doAnswer((Answer<Void>) invocationOnMock -> {
      DebtPositionDTO debtPosition = invocationOnMock.getArgument(0);
      InstallmentDTO installment = debtPosition.getPaymentOptions().getFirst().getInstallments().getFirst();
      installment.setTransfers(List.of(
        TransferDTO.builder()
          .transferIndex(1)
          .orgFiscalCode("12345678901")
          .orgName("Beneficiary Org")
          .amountCents(200L)
          .remittanceInformation("causale")
          .iban("IT60X0542811101000000123456")
          .category(expectedCategory)
          .build()
      ));
      return null;
    }).when(secondaryTransferMapperMock).fillSecondaryTransferData(any(), any());

    //when
    PaymentRequestMappingResult paymentRequestMappingResult = mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN);
    List<DebtPositionDTO> result = paymentRequestMappingResult.debtPositions();

    //then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(1, result.getFirst().getPaymentOptions().size());
    assertEquals(1, result.getFirst().getPaymentOptions().getFirst().getInstallments().size());
    assertEquals(1, result.getFirst().getPaymentOptions().getFirst().getInstallments().getFirst().getTransfers().size());
    TransferDTO firstTransfer = result.getFirst().getPaymentOptions().getFirst().getInstallments().getFirst().getTransfers().getFirst();
    assertEquals(expectedCategory, firstTransfer.getCategory());

    result.forEach(dp -> {
      TestUtils.checkNotNullFields(dp,
        "debtPositionId", "validityDate", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
      dp.getPaymentOptions().forEach(po -> {
        TestUtils.checkNotNullFields(po, "paymentOptionId", "debtPositionId", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId", "description");
        po.getInstallments().forEach(i -> {
          TestUtils.checkNotNullFields(i, "installmentId", "paymentOptionId", "syncStatus", "iupdPagopa", "generateNotice",
            "iuv", "iur", "iuf", "nav", "iun", "notificationFeeCents", "transfers", "notificationDate", "ingestionFlowFileId",
            "ingestionFlowFileAction", "ingestionFlowFileLineNumber", "receiptId", "switchToExpired",
            "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
          if (i.getTransfers() != null) {
            i.getTransfers().forEach(t -> {
              TestUtils.checkNotNullFields(t, "transferId", "installmentId",
                "stampType", "stampHashDocument", "stampProvincialResidence", "postalIban",
                "mbdAttachment",
                "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId", "mbdAttachment");
            });
          }
        });
      });
    });
  }

  @Test
  void whenMapRequestToDebtPositionsThenReturnMixedDebtPosition() {
    // given
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    request.setListaDovuti(new ListaDovuti());
    request.getListaDovuti().getElementoListaDovutis().add(new ElementoListaDovuti());
    request.getListaDovuti().getElementoListaDovutis().getFirst().setDovuti(new byte[]{});
    Dovuti dovuti = podamFactory.manufacturePojo(Dovuti.class);
    dovuti.setSoggettoPagatore(new CtSoggettoPagatore());
    CtDatiSingoloVersamentoDovuti versamento1 = new CtDatiSingoloVersamentoDovuti();
    versamento1.setCausaleVersamento("causale");
    versamento1.setImportoSingoloVersamento(BigDecimal.TWO);
    versamento1.setDatiSpecificiRiscossione("9/DATI_SPECIFICI");
    versamento1.setIdentificativoTipoDovuto("COD_TIPO_DOVUTO");
    versamento1.setIdentificativoUnivocoDovuto("IUD");
    Bilancio bilancio = podamFactory.manufacturePojo(Bilancio.class);
    versamento1.setBilancio(bilancio);
    when(jaxbTransformServiceMock.marshalling(bilancio, Bilancio.class)).thenReturn("bilancioString");
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().clear();
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().add(versamento1);
    dovuti.getDatiVersamento().setIdentificativoUnivocoVersamento(null);
    CtDatiSingoloVersamentoDovuti versamento2 = new CtDatiSingoloVersamentoDovuti();
    versamento2.setCausaleVersamento("causale");
    versamento2.setImportoSingoloVersamento(BigDecimal.ONE);
    versamento2.setIdentificativoTipoDovuto("NO-IBAN");
    versamento2.setIdentificativoUnivocoDovuto("IUD-NO-IBAN");
    versamento2.setDatiSpecificiRiscossione("9/1234567IM/");
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().add(versamento2);

    DebtPositionTypeOrg debtPositionTypeOrg1 = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);
    DebtPositionTypeOrg debtPositionTypeOrg2 = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);

    PersonDTO debtor = podamFactory.manufacturePojo(PersonDTO.class);
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenReturn(dovuti);
    doNothing().when(validationServiceMock).validateCartSize(request.getListaDovuti().getElementoListaDovutis().size());
    when(personMapperMock.getAndValidateDebtor(any())).thenReturn(debtor);

    doReturn(debtPositionTypeOrg1).when(debtPositionTypeServiceMock)
      .getDebtPositionTypeOrgByOrgIdAndType(org.getOrganizationId(), versamento1.getIdentificativoTipoDovuto(), ACCESS_TOKEN);
    doReturn(debtPositionTypeOrg2).when(debtPositionTypeServiceMock)
      .getDebtPositionTypeOrgByOrgIdAndType(org.getOrganizationId(), versamento2.getIdentificativoTipoDovuto(), ACCESS_TOKEN);

    //when
    PaymentRequestMappingResult paymentRequestMappingResult = mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN);
    List<MixedDebtPositionDTO> result = paymentRequestMappingResult.mixedDebtPositions();

    //then
    assertNotNull(result);
    assertEquals(1, result.size());
    MixedDebtPositionDTO mixedDebtPositionDTO = result.getFirst();
    TestUtils.checkAllNotNullFields(mixedDebtPositionDTO);

    List<MixedTransferDTO> mixedTransfers = mixedDebtPositionDTO.getTransfers();
    assertEquals(2, mixedTransfers.size());

    mixedTransfers.forEach(mt -> {
      assertNull(mt.getStampHashDocument());
      assertNull(mt.getStampType());
      assertNull(mt.getStampProvincialResidence());
      TestUtils.checkAllNotNullFields(mt,
        "stampType", "stampHashDocument", "stampProvincialResidence", "balance");
    });
  }
}

