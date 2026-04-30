package it.gov.pagopa.pu.sil.mapper;


import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.service.debtposition.DebtPositionInstallmentService;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecondaryTransferMapperTest {

  @Mock
  JAXBTransformService jaxbTransformServiceMock;

  @InjectMocks
  SecondaryTransferMapper secondaryTransferMapper;

  @Mock
  DebtPositionInstallmentService debtPositionInstallmentServiceMock;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  private static final String ACCESS_TOKEN = "token";

  @BeforeEach
  void setup() {
    Mockito.reset(jaxbTransformServiceMock);
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
  void mapToCtDatiVersamentoDovutiEntiSecondari_shouldReturnOptional() {
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
  void mapToCtDatiVersamentoDovutiEntiSecondari_shouldThrowOnUnmarshalException() {
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
  @Test
  void fillSecondaryTransferData_shouldFillCorrectly() {
    int defaultCollectionSize = podamFactory.getStrategy().getNumberOfCollectionElements(Object.class);
    podamFactory.getStrategy().setDefaultNumberOfCollectionElements(1);
    DebtPositionDTO debtPosition = podamFactory.manufacturePojo(DebtPositionDTO.class);
    debtPosition.getPaymentOptions().getFirst().getInstallments().getFirst().setTransfers(null);
    podamFactory.getStrategy().setDefaultNumberOfCollectionElements(defaultCollectionSize);
    CtDatiVersamentoDovutiEntiSecondari secondaryTransferData = podamFactory.manufacturePojo(CtDatiVersamentoDovutiEntiSecondari.class);
    BigDecimal scaled = secondaryTransferData.getImportoSingoloVersamento().setScale(2, RoundingMode.HALF_UP);
    secondaryTransferData.setImportoSingoloVersamento(scaled);
    secondaryTransferData.setDatiSpecificiRiscossione("9/1234567IM/legacyMetadata");
    when(debtPositionInstallmentServiceMock.getCategory(secondaryTransferData.getDatiSpecificiRiscossione(), "code", debtPosition.getOrganizationId(), ACCESS_TOKEN)).thenReturn("9/1234567IM/");

    assertDoesNotThrow(() -> secondaryTransferMapper.fillSecondaryTransferData(debtPosition, secondaryTransferData, "code", ACCESS_TOKEN));

    TestUtils.checkNotNullFields(debtPosition.getPaymentOptions().getFirst().getInstallments().getFirst().getTransfers().getFirst(),
      "transferId", "installmentId", "stampType", "stampHashDocument", "stampProvincialResidence", "mbdAttachment", "postalIban", "flagOwner",
      "creationDate", "updateDate", "updateOperatorExternalId", "updateTraceId");
  }

  //endregion


  //region: checkAndFillSupportedTransfersConfigurationForModify
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void checkAndFillSupportedTransfersConfigurationForModify_singleTransfer(boolean legacyMode) {
    InstallmentDTO installmentOnDb = podamFactory.manufacturePojo(InstallmentDTO.class);
    TransferDTO transferOnDb = podamFactory.manufacturePojo(TransferDTO.class).transferIndex(1);
    installmentOnDb.setTransfers(List.of(transferOnDb));
    InstallmentDTO installmentToSync = installmentOnDb.toBuilder().build();
    if (legacyMode) {
      installmentToSync.setTransfers(null);
    } else {
      TransferDTO transferToSync = transferOnDb.toBuilder().amountCents(transferOnDb.getAmountCents() + 100L).remittanceInformation("new info").build();
      installmentToSync.setTransfers(List.of(transferToSync));
    }

    assertDoesNotThrow(() ->
      secondaryTransferMapper.checkAndFillSupportedTransfersConfigurationForModify(installmentOnDb, installmentToSync, legacyMode));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void checkAndFillSupportedTransfersConfigurationForModify_shouldThrowOnSizeMismatch(boolean legacyMode) {
    InstallmentDTO installmentOnDb = podamFactory.manufacturePojo(InstallmentDTO.class);
    installmentOnDb.setTransfers(Stream.of(1, 2, 3).map(i -> podamFactory.manufacturePojo(TransferDTO.class).transferIndex(i)).toList());
    InstallmentDTO installmentToSync = installmentOnDb.toBuilder().build();
    installmentToSync.setTransfers(Stream.of(1).map(i -> podamFactory.manufacturePojo(TransferDTO.class).transferIndex(i)).toList());

    assertThrows(SilFaultException.class, () ->
      secondaryTransferMapper.checkAndFillSupportedTransfersConfigurationForModify(installmentOnDb, installmentToSync, legacyMode));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void checkAndFillSupportedTransfersConfigurationForModify_shouldUpdateFields(boolean legacyMode) {
    InstallmentDTO installmentOnDb = new InstallmentDTO();
    TransferDTO t1 = new TransferDTO()
      .transferIndex(1)
      .amountCents(10L)
      .orgFiscalCode("CF")
      .orgName("NAME")
      .category("CAT")
      .remittanceInformation("old");
    TransferDTO t2 = new TransferDTO()
      .transferIndex(2)
      .orgFiscalCode("CF")
      .category("CAT")
      .iban("IBAN")
      .postalIban("POSTAL")
      .amountCents(20L)
      .remittanceInformation("old2");
    installmentOnDb.setTransfers(List.of(t1, t2));
    installmentOnDb.setInstallmentId(1L);

    InstallmentDTO installmentToSync = new InstallmentDTO();
    installmentToSync.setAmountCents(100L);
    installmentToSync.setRemittanceInformation("new1");
    TransferDTO t2Sync = new TransferDTO()
      .transferIndex(2)
      .orgFiscalCode("CF")
      .category("CAT")
      .iban("IBAN")
      .postalIban("POSTAL")
      .amountCents(90L)
      .remittanceInformation("new2");
    if (legacyMode) {
      installmentToSync.setTransfers(List.of(t2Sync));
    } else {
      TransferDTO t1Sync = installmentOnDb.getTransfers().getFirst().toBuilder().remittanceInformation("new1").build();
      installmentToSync.setTransfers(List.of(t1Sync, t2Sync));
    }

    assertDoesNotThrow(() ->
      secondaryTransferMapper.checkAndFillSupportedTransfersConfigurationForModify(installmentOnDb, installmentToSync, legacyMode));
    assertEquals(10L, t1.getAmountCents()); // updated in method
    assertEquals("new1", t1.getRemittanceInformation());
    assertEquals(90L, t2.getAmountCents());
    assertEquals("new2", t2.getRemittanceInformation());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void checkAndFillSupportedTransfersConfigurationForModify_shouldThrowOnFieldMismatch(boolean legacyMode) {
    InstallmentDTO installmentOnDb = new InstallmentDTO();
    TransferDTO t1 = new TransferDTO()
      .transferIndex(1)
      .amountCents(10L)
      .orgFiscalCode("CF")
      .orgName("NAME")
      .category("CAT")
      .remittanceInformation("old");
    TransferDTO t2 = new TransferDTO()
      .transferIndex(2)
      .orgFiscalCode("CF")
      .category("CAT")
      .iban("IBAN")
      .postalIban("POSTAL")
      .amountCents(20L)
      .remittanceInformation("old2");
    installmentOnDb.setTransfers(List.of(t1, t2));
    installmentOnDb.setInstallmentId(1L);

    InstallmentDTO installmentToSync = new InstallmentDTO();
    installmentToSync.setAmountCents(100L);
    TransferDTO t2Sync = new TransferDTO()
      .transferIndex(2)
      .orgFiscalCode("CF2") // different
      .category("CAT")
      .iban("IBAN")
      .postalIban("POSTAL")
      .amountCents(90L)
      .remittanceInformation("new1");
    installmentToSync.setRemittanceInformation("new1");


    if (legacyMode) {
      installmentToSync.setTransfers(List.of(t2Sync));
    } else {
      TransferDTO t1Sync = installmentOnDb.getTransfers().getFirst().toBuilder().build();
      installmentToSync.setTransfers(List.of(t1Sync, t2Sync));
    }

    assertThrows(SilFaultException.class, () ->
      secondaryTransferMapper.checkAndFillSupportedTransfersConfigurationForModify(installmentOnDb, installmentToSync, legacyMode));
  }
  //endregion

}
