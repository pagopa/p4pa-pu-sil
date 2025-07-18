package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationStatus;
import it.gov.pagopa.pu.organization.dto.generated.Taxonomy;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.organization.service.TaxonomyService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.immediatepayments.ValidationService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.*;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiSingoloVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtSoggettoPagatore;
import it.veneto.regione.schemas._2012.pagamenti.ente.Dovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.DovutiEntiSecondari;
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
  private TaxonomyService taxonomyServiceMock;

  @Mock
  private PersonMapper personMapperMock;

  @Mock
  private ValidationService validationServiceMock;

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

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class,() -> mapper.mapRequestToDebtPositions(request, org,"CART_ID", ACCESS_TOKEN));

    assertEquals(SilFaults.PAA_XML_NON_VALIDO, exception.getFault());
    assertTrue(exception.getDescription().contains("XML dovuti [1] non conforme"));
  }

  @Test
  void mapRequestToDebtPositions_InvalidPrimaryEnte_ReturnsError() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    doThrow(new SilFaultException(SilFaults.PAA_ENTE_NON_VALIDO, "error")).when(validationServiceMock).validatePrimaryDebtPositionOrganization(request, ORG_IPA_CODE);

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class,() -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

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

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class,() -> mapper.mapRequestToDebtPositions(request, org,"CART_ID", ACCESS_TOKEN));

    assertEquals(SilFaults.PAA_ENTE_NON_VALIDO, exception.getFault());
    assertEquals("error", exception.getDescription());
  }

  @Test
  void mapRequestToDebtPositions_maxCartSize_ReturnsError() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    request.setListaDovuti(new ListaDovuti());
    for(int i = 0; i < 8; i++){
      ElementoListaDovuti elem = new ElementoListaDovuti();
      elem.setDovuti(new byte[]{});
      request.getListaDovuti().getElementoListaDovutis().add(elem);
    }

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class,() -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

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
    dovuti.getDatiVersamento().setIdentificativoUnivocoVersamento("IUV");
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenReturn(dovuti);

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class,() -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

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
    dovuti.getDatiVersamento().setIdentificativoUnivocoVersamento(null);
    dovuti.setSoggettoPagatore(new CtSoggettoPagatore());
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenReturn(dovuti);

   doThrow(new SilFaultException(SilFaults.PAA_ANAGRAFICA_NON_VALIDA, "error")).when(personMapperMock).getAndValidateDebtor(any());

    SilFaultException exception = Assertions.assertThrows(SilFaultException.class,() -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

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
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(DovutiEntiSecondari.class), any())).thenReturn(dovutiEntiSecondari);
    when(personMapperMock.getAndValidateDebtor(any())).thenReturn(debtor);
    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(
      org.getOrganizationId(), versamento.getIdentificativoTipoDovuto(), ACCESS_TOKEN)).thenReturn(debtPositionTypeOrg);
    if(legacyPaymentMetadataSecondary == null) {
      when(debtPositionTypeServiceMock.getDebtPositionTypeById(debtPositionType.getDebtPositionTypeId(), ACCESS_TOKEN)).thenReturn(debtPositionType);
    }
    String expectedCategory = legacyPaymentMetadataSecondary==null ? debtPositionType.getTaxonomyCode() : legacyPaymentMetadataSecondary.substring(2,11);
    when(taxonomyServiceMock.getTaxonomyByTaxonomyCode(expectedCategory, ACCESS_TOKEN)).thenReturn(Optional.of(new Taxonomy()));

    //when
    List<DebtPositionDTO> result = mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN);

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
        TestUtils.checkNotNullFields(po, "paymentOptionId", "debtPositionId", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
        po.getInstallments().forEach(i -> {
          TestUtils.checkNotNullFields(i, "installmentId", "paymentOptionId", "syncStatus", "iupdPagopa",
            "iuv", "iur", "iuf", "nav", "iun", "notificationFeeCents", "transfers", "notificationDate", "ingestionFlowFileId",
            "ingestionFlowFileAction", "ingestionFlowFileLineNumber", "receiptId",
            "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
          if(i.getTransfers()!=null) {
            i.getTransfers().forEach(t -> {
              TestUtils.checkNotNullFields(t, "transferId", "installmentId",
                "stampType", "stampHashDocument", "stampProvincialResidence", "postalIban",
                "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId", "mbdAttachment");
            });
          }
        });
      });
    });

  }

  @ParameterizedTest
  @ValueSource(strings = {"9/1234567IM/"})
  @NullSource
  void mapRequestToDebtPositions_InvalidTaxonomy_ReturnsError(String legacyPaymentMetadataSecondary) {
    //given
    DebtPositionTypeOrg debtPositionTypeOrg = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);
    DebtPositionType debtPositionType = podamFactory.manufacturePojo(DebtPositionType.class);
    debtPositionTypeOrg.setDebtPositionTypeId(debtPositionType.getDebtPositionTypeId());

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
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(DovutiEntiSecondari.class), any())).thenReturn(dovutiEntiSecondari);
    when(personMapperMock.getAndValidateDebtor(any())).thenReturn(debtor);
    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(
      org.getOrganizationId(), versamento.getIdentificativoTipoDovuto(), ACCESS_TOKEN)).thenReturn(debtPositionTypeOrg);
    if(legacyPaymentMetadataSecondary == null) {
      when(debtPositionTypeServiceMock.getDebtPositionTypeById(debtPositionType.getDebtPositionTypeId(), ACCESS_TOKEN)).thenReturn(new DebtPositionType());
    } else {
      when(taxonomyServiceMock.getTaxonomyByTaxonomyCode(legacyPaymentMetadataSecondary.substring(2,11), ACCESS_TOKEN)).thenReturn(Optional.empty());
    }

    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class,() -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

    assertEquals(SilFaults.PAA_ENTE_SECONDARIO_NON_VALIDO, exception.getFault());
    assertTrue(exception.getDescription().contains("Codice tassonomico"));
  }

  @Test
  void mapRequestToDebtPositions_ErrorUnmarshallSecodary_ReturnsError() {
    //given
    DebtPositionTypeOrg debtPositionTypeOrg = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);
    DebtPositionType debtPositionType = podamFactory.manufacturePojo(DebtPositionType.class);
    debtPositionTypeOrg.setDebtPositionTypeId(debtPositionType.getDebtPositionTypeId());

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
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().clear();
    dovuti.getDatiVersamento().getDatiSingoloVersamentos().add(versamento);
    dovuti.getDatiVersamento().setIdentificativoUnivocoVersamento(null);
    request.setListaDovutiEntiSecondari(new ListaDovutiEntiSecondari());
    request.getListaDovutiEntiSecondari().getElementoListaDovutiEntiSecondaris().add(new ElementoListaDovutiEntiSecondari());
    request.getListaDovutiEntiSecondari().getElementoListaDovutiEntiSecondaris().getFirst().setDovutiEntiSecondari(new byte[]{});
    PersonDTO debtor = podamFactory.manufacturePojo(PersonDTO.class);
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(Dovuti.class), any())).thenReturn(dovuti);
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(DovutiEntiSecondari.class), any())).thenThrow(new ApplicationException("error unmarshalling secondary"));
    when(personMapperMock.getAndValidateDebtor(any())).thenReturn(debtor);
    when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(
      org.getOrganizationId(), versamento.getIdentificativoTipoDovuto(), ACCESS_TOKEN)).thenReturn(debtPositionTypeOrg);

    //when
    SilFaultException exception = Assertions.assertThrows(SilFaultException.class,() -> mapper.mapRequestToDebtPositions(request, org, "CART_ID", ACCESS_TOKEN));

    assertEquals(SilFaults.PAA_XML_NON_VALIDO, exception.getFault());
    assertTrue(exception.getDescription().contains("XML dovuti enti secondari non conforme"));
  }
}

