package it.gov.pagopa.pu.sil.mapper;


import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Taxonomy;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionTypeService;
import it.gov.pagopa.pu.sil.connector.organization.service.TaxonomyService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.ElementoListaDovutiEntiSecondari;
import it.veneto.regione.pagamenti.ente.ListaDovutiEntiSecondari;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiVersamentoDovutiEntiSecondari;
import it.veneto.regione.schemas._2012.pagamenti.ente.DovutiEntiSecondari;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecondaryTransferMapperTest {

  @Mock
  TaxonomyService taxonomyServiceMock;
  @Mock
  JAXBTransformService jaxbTransformServiceMock;
  @Mock
  DebtPositionTypeService debtPositionTypeServiceMock;

  @InjectMocks
  SecondaryTransferMapper secondaryTransferMapper;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setup() {
    Mockito.reset(taxonomyServiceMock, jaxbTransformServiceMock, debtPositionTypeServiceMock);
  }

  //region: mapToCtDatiVersamentoDovutiEntiSecondari
  @Test
  void mapToCtDatiVersamentoDovutiEntiSecondari_shouldReturnEmptyIfNull() {
    assertTrue(secondaryTransferMapper.mapToCtDatiVersamentoDovutiEntiSecondari(null).isEmpty());
  }

  @Test
  void mapToCtDatiVersamentoDovutiEntiSecondari_shouldReturnEmptyIfListEmpty() {
    ListaDovutiEntiSecondari list = new ListaDovutiEntiSecondari();
    assertTrue(secondaryTransferMapper.mapToCtDatiVersamentoDovutiEntiSecondari(list).isEmpty());
  }

  @Test
  void mapToCtDatiVersamentoDovutiEntiSecondari_shouldThrowIfMoreThanOne() {
    ListaDovutiEntiSecondari list = new ListaDovutiEntiSecondari();
    list.getElementoListaDovutiEntiSecondaris().add(new ElementoListaDovutiEntiSecondari());
    list.getElementoListaDovutiEntiSecondaris().add(new ElementoListaDovutiEntiSecondari());
    assertThrows(SilFaultException.class, () ->
      secondaryTransferMapper.mapToCtDatiVersamentoDovutiEntiSecondari(list));
  }

  @Test
  void mapToCtDatiVersamentoDovutiEntiSecondari_shouldReturnOptional() throws Exception {
    ListaDovutiEntiSecondari list = new ListaDovutiEntiSecondari();
    ElementoListaDovutiEntiSecondari el = new ElementoListaDovutiEntiSecondari();
    el.setDovutiEntiSecondari(new byte[]{1, 2, 3});
    list.getElementoListaDovutiEntiSecondaris().add(el);

    DovutiEntiSecondari dovutiEntiSecondari = mock(DovutiEntiSecondari.class);
    CtDatiVersamentoDovutiEntiSecondari dati = mock(CtDatiVersamentoDovutiEntiSecondari.class);
    when(dovutiEntiSecondari.getDatiVersamentoEntiSecondari()).thenReturn(dati);
    when(jaxbTransformServiceMock.unmarshalling(any(), eq(DovutiEntiSecondari.class), any()))
      .thenReturn(dovutiEntiSecondari);

    Optional<CtDatiVersamentoDovutiEntiSecondari> result =
      secondaryTransferMapper.mapToCtDatiVersamentoDovutiEntiSecondari(list);

    assertTrue(result.isPresent());
    assertEquals(dati, result.get());
  }

  @Test
  void mapToCtDatiVersamentoDovutiEntiSecondari_shouldThrowOnUnmarshalException() throws Exception {
    ListaDovutiEntiSecondari list = new ListaDovutiEntiSecondari();
    ElementoListaDovutiEntiSecondari el = new ElementoListaDovutiEntiSecondari();
    el.setDovutiEntiSecondari(new byte[]{1, 2, 3});
    list.getElementoListaDovutiEntiSecondaris().add(el);

    when(jaxbTransformServiceMock.unmarshalling(any(), eq(DovutiEntiSecondari.class), any()))
      .thenThrow(new ApplicationException("fail"));
    when(jaxbTransformServiceMock.getDetailUnmarshalExceptionMessage(any(), any()))
      .thenReturn("details");

    assertThrows(SilFaultException.class, () ->
      secondaryTransferMapper.mapToCtDatiVersamentoDovutiEntiSecondari(list));
  }
  //endregion

  //region: fillSecondaryTransferData
  @ParameterizedTest
  @ValueSource(strings = {"validCategoryFromLegacyPaymentMetadata", "validCategoryFromDebtPositionOrgCode"})
  void fillSecondaryTransferData_shouldFillCorrectly(String testCase) {
    int defaultCollectionSize = podamFactory.getStrategy().getNumberOfCollectionElements(Object.class);
    podamFactory.getStrategy().setDefaultNumberOfCollectionElements(1);
    DebtPositionDTO debtPosition = podamFactory.manufacturePojo(DebtPositionDTO.class);
    debtPosition.getPaymentOptions().getFirst().getInstallments().getFirst().setTransfers(null);
    podamFactory.getStrategy().setDefaultNumberOfCollectionElements(defaultCollectionSize);
    CtDatiVersamentoDovutiEntiSecondari secondaryTransferData = podamFactory.manufacturePojo(CtDatiVersamentoDovutiEntiSecondari.class);
    if (testCase.equals("validCategoryFromLegacyPaymentMetadata")) {
      secondaryTransferData.setDatiSpecificiRiscossione("9/1234567IM/legacyMetadata");
    } else if (testCase.equals("validCategoryFromDebtPositionOrgCode")) {
      DebtPositionTypeOrg debtPositionTypeOrg = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);
      DebtPositionType debtPositionType = podamFactory.manufacturePojo(DebtPositionType.class);
      debtPositionType.setTaxonomyCode("1234567IM");
      when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(debtPosition.getOrganizationId(), "ORG_CODE", "accessToken"))
        .thenReturn(debtPositionTypeOrg);
      when(debtPositionTypeServiceMock.getDebtPositionTypeById(debtPositionTypeOrg.getDebtPositionTypeId(), "accessToken"))
        .thenReturn(debtPositionType);
    }
    Taxonomy taxonomy = podamFactory.manufacturePojo(Taxonomy.class);
    when(taxonomyServiceMock.getTaxonomyByTaxonomyCode("1234567IM", "accessToken"))
      .thenReturn(Optional.of(taxonomy));

    assertDoesNotThrow(() -> secondaryTransferMapper.fillSecondaryTransferData(debtPosition, secondaryTransferData, "ORG_CODE", "accessToken"));

    TestUtils.checkNotNullFields(debtPosition.getPaymentOptions().getFirst().getInstallments().getFirst().getTransfers().getFirst(),
      "transferId", "installmentId", "stampType", "stampHashDocument", "stampProvincialResidence", "mbdAttachment", "postalIban", "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalidCategoryFromLegacyPaymentMetadata", "invalidCategoryFromDebtPositionOrgCode", "invalidCategory", "invalidNumberOfTransfers"})
  void fillSecondaryTransferData_shouldThrowException(String testCase) {
    int defaultCollectionSize = podamFactory.getStrategy().getNumberOfCollectionElements(Object.class);
    podamFactory.getStrategy().setDefaultNumberOfCollectionElements(1);
    DebtPositionDTO debtPosition = podamFactory.manufacturePojo(DebtPositionDTO.class);
    podamFactory.getStrategy().setDefaultNumberOfCollectionElements(defaultCollectionSize);
    CtDatiVersamentoDovutiEntiSecondari secondaryTransferData = podamFactory.manufacturePojo(CtDatiVersamentoDovutiEntiSecondari.class);
    secondaryTransferData.setDatiSpecificiRiscossione("9/1234567IM/legacyMetadata");
    Taxonomy taxonomy = podamFactory.manufacturePojo(Taxonomy.class);
    SilFaults expectedFault = SilFaults.PAA_ENTE_SECONDARIO_NON_VALIDO;
    if (testCase.equals("invalidCategory")) {
      taxonomy = null;
    }
    if (testCase.equals("invalidCategoryFromLegacyPaymentMetadata") || testCase.equals("invalidCategoryFromDebtPositionOrgCode")) {
      secondaryTransferData.setDatiSpecificiRiscossione(testCase.equals("invalidCategoryFromLegacyPaymentMetadata") ? "9/1234IM/legacyMetadata" : null);
      DebtPositionTypeOrg debtPositionTypeOrg = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);
      DebtPositionType debtPositionType = podamFactory.manufacturePojo(DebtPositionType.class);
      debtPositionType.setTaxonomyCode("");
      when(debtPositionTypeServiceMock.getDebtPositionTypeOrgByOrgIdAndType(debtPosition.getOrganizationId(), "ORG_CODE", "accessToken"))
        .thenReturn(debtPositionTypeOrg);
      when(debtPositionTypeServiceMock.getDebtPositionTypeById(debtPositionTypeOrg.getDebtPositionTypeId(), "accessToken"))
        .thenReturn(debtPositionType);
    } else {
      when(taxonomyServiceMock.getTaxonomyByTaxonomyCode("1234567IM", "accessToken"))
        .thenReturn(Optional.ofNullable(taxonomy));
    }
    if (testCase.equals("invalidNumberOfTransfers")) {
      expectedFault = SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_MULTIBENEFICIARI;
    } else {
      debtPosition.getPaymentOptions().getFirst().getInstallments().getFirst().setTransfers(null);
    }


    SilFaultException exception = assertThrows(SilFaultException.class, () -> secondaryTransferMapper.fillSecondaryTransferData(debtPosition, secondaryTransferData, "ORG_CODE", "accessToken"));

    assertEquals(expectedFault, exception.getFault());
  }
  //endregion


  //region: checkAndFillSupportedTransfersConfigurationForModify
  @Test
  void checkAndFillSupportedTransfersConfigurationForModify_singleTransfer() {
    InstallmentDTO installmentOnDb = new InstallmentDTO();
    installmentOnDb.setTransfers(List.of(new TransferDTO()));
    InstallmentDTO installmentToSync = new InstallmentDTO();
    installmentToSync.setTransfers(null);

    assertDoesNotThrow(() ->
      secondaryTransferMapper.checkAndFillSupportedTransfersConfigurationForModify(installmentOnDb, installmentToSync));
  }

  @Test
  void checkAndFillSupportedTransfersConfigurationForModify_shouldThrowOnSizeMismatch() {
    InstallmentDTO installmentOnDb = new InstallmentDTO();
    installmentOnDb.setTransfers(List.of(new TransferDTO(), new TransferDTO(), new TransferDTO()));
    InstallmentDTO installmentToSync = new InstallmentDTO();
    installmentToSync.setTransfers(List.of(new TransferDTO()));

    assertThrows(SilFaultException.class, () ->
      secondaryTransferMapper.checkAndFillSupportedTransfersConfigurationForModify(installmentOnDb, installmentToSync));
  }

  @Test
  void checkAndFillSupportedTransfersConfigurationForModify_shouldUpdateFields() {
    InstallmentDTO installmentOnDb = new InstallmentDTO();
    TransferDTO t1 = new TransferDTO();
    t1.setTransferIndex(1);
    t1.setAmountCents(10L);
    t1.setRemittanceInformation("old");
    TransferDTO t2 = new TransferDTO();
    t2.setTransferIndex(2);
    t2.setOrgFiscalCode("CF");
    t2.setCategory("CAT");
    t2.setIban("IBAN");
    t2.setPostalIban("POSTAL");
    t2.setAmountCents(20L);
    t2.setRemittanceInformation("old2");
    installmentOnDb.setTransfers(List.of(t1, t2));
    installmentOnDb.setInstallmentId(1L);

    InstallmentDTO installmentToSync = new InstallmentDTO();
    installmentToSync.setAmountCents(100L);
    TransferDTO t2Sync = new TransferDTO();
    t2Sync.setTransferIndex(2);
    t2Sync.setOrgFiscalCode("CF");
    t2Sync.setCategory("CAT");
    t2Sync.setIban("IBAN");
    t2Sync.setPostalIban("POSTAL");
    t2Sync.setAmountCents(90L);
    t2Sync.setRemittanceInformation("new2");
    installmentToSync.setTransfers(List.of(t2Sync));
    installmentToSync.setRemittanceInformation("new1");

    assertDoesNotThrow(() ->
      secondaryTransferMapper.checkAndFillSupportedTransfersConfigurationForModify(installmentOnDb, installmentToSync));
    assertEquals(10L, t1.getAmountCents()); // updated in method
    assertEquals("new1", t1.getRemittanceInformation());
    assertEquals(90L, t2.getAmountCents());
    assertEquals("new2", t2.getRemittanceInformation());
  }

  @Test
  void checkAndFillSupportedTransfersConfigurationForModify_shouldThrowOnFieldMismatch() {
    InstallmentDTO installmentOnDb = new InstallmentDTO();
    TransferDTO t1 = new TransferDTO();
    t1.setTransferIndex(1);
    TransferDTO t2 = new TransferDTO();
    t2.setTransferIndex(2);
    t2.setOrgFiscalCode("CF1");
    t2.setCategory("CAT");
    t2.setIban("IBAN");
    t2.setPostalIban("POSTAL");
    installmentOnDb.setTransfers(List.of(t1, t2));
    installmentOnDb.setInstallmentId(1L);

    InstallmentDTO installmentToSync = new InstallmentDTO();
    installmentToSync.setAmountCents(100L);
    TransferDTO t2Sync = new TransferDTO();
    t2Sync.setTransferIndex(2);
    t2Sync.setOrgFiscalCode("CF2"); // different
    t2Sync.setCategory("CAT");
    t2Sync.setIban("IBAN");
    t2Sync.setPostalIban("POSTAL");
    t2Sync.setAmountCents(90L);
    installmentToSync.setTransfers(List.of(t2Sync));
    installmentToSync.setRemittanceInformation("new1");

    assertThrows(SilFaultException.class, () ->
      secondaryTransferMapper.checkAndFillSupportedTransfersConfigurationForModify(installmentOnDb, installmentToSync));
  }
  //endregion

}
