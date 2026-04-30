package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.organization.dto.generated.Organization;
import it.gov.pagopa.pu.sil.connector.debtpositions.ReceiptService;
import it.gov.pagopa.pu.sil.service.soap.JAXBTransformService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.schemas._2012.pagamenti.ente.Pagati;
import it.veneto.regione.schemas._2012.pagamenti.ente.PagatiConRicevuta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;
import java.util.stream.Stream;

import static it.gov.pagopa.pu.sil.util.Constants.INSTALLMENT_REMITTANCE_INFORMATION_PLACEHOLDER;

@ExtendWith(MockitoExtension.class)
class PagatiMapperTest {

  @InjectMocks
  private PagatiMapper pagatiMapper;

  @Mock
  private JAXBTransformService jaxbTransformServiceMock;
  @Mock
  private ReceiptService receiptServiceMock;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @ParameterizedTest
  @ValueSource(strings={"default", "mdb"})
  void testMapDebtPositionsToEncodedPagatiConRicevuta(String testType) {
    // Given
    podamFactory.getStrategy().setDefaultNumberOfCollectionElements(2);
    DebtPositionDTO debtPosition = podamFactory.manufacturePojo(DebtPositionDTO.class);
    debtPosition.getPaymentOptions()
      .stream().map(PaymentOptionDTO::getInstallments)
      .flatMap(List::stream)
      .forEach(installment -> installment.setTransfers(List.of(installment.getTransfers().getFirst())));
    InstallmentDTO installment = debtPosition.getPaymentOptions().getFirst().getInstallments().get(1);
    Organization organization = podamFactory.manufacturePojo(Organization.class);
    debtPosition.setOrganizationId(organization.getOrganizationId());
    ReceiptDTO receipt = podamFactory.manufacturePojo(ReceiptDTO.class);
    receipt.setCreditorReferenceId(installment.getIuv());
    if(!testType.equals("mdb")){
      installment.getTransfers().getFirst().setStampHashDocument(null);
    }
    byte[] expectedBytes = "expectedBytes".getBytes();

    Mockito.when(receiptServiceMock.getReceiptById(installment.getReceiptId(), "accessToken"))
      .thenReturn(receipt);

    Mockito.when(jaxbTransformServiceMock.marshallingAsBytes(Mockito.argThat(p -> {
      Assertions.assertNotNull(p);
      TestUtils.checkNotNullFields(p, "riferimentoDataRichiesta");
      TestUtils.checkNotNullFields(p.getDominio(), "identificativoStazioneRichiedente");
      TestUtils.checkNotNullFields(p.getEnteBeneficiario(),"codiceUnitOperBeneficiario",
        "denomUnitOperBeneficiario",
        "indirizzoBeneficiario",
        "civicoBeneficiario",
        "capBeneficiario",
        "localitaBeneficiario",
        "provinciaBeneficiario",
        "nazioneBeneficiario");
      TestUtils.checkNotNullFields(p.getEnteBeneficiario().getIdentificativoUnivocoBeneficiario());
      TestUtils.checkNotNullFields(p.getIstitutoAttestante(),"codiceUnitOperAttestante",
        "denomUnitOperAttestante",
        "indirizzoAttestante",
        "civicoAttestante",
        "capAttestante",
        "localitaAttestante",
        "provinciaAttestante",
        "nazioneAttestante");
      TestUtils.checkNotNullFields(p.getIstitutoAttestante().getIdentificativoUnivocoAttestante());
      TestUtils.checkNotNullFields(p.getSoggettoPagatore());
      TestUtils.checkNotNullFields(p.getSoggettoPagatore().getIdentificativoUnivocoPagatore());
      TestUtils.checkNotNullFields(p.getDatiPagamento());
      String[] excludedFields = testType.equals("mdb") ? new String[]{} : new String[]{"allegatoRicevuta"};
      p.getDatiPagamento().getDatiSingoloPagamentos().forEach(dp -> {
        TestUtils.checkNotNullFields(dp, excludedFields);
      });

      Assertions.assertEquals(installment.getIuv(), p.getDatiPagamento().getIdentificativoUnivocoVersamento());

      return true;
    }), Mockito.eq(PagatiConRicevuta.class))).thenReturn(expectedBytes);

    // When
    byte[] result = pagatiMapper.mapDebtPositionsToEncodedPagatiConRicevuta(installment, organization, "accessToken");

    // Then
    Assertions.assertEquals(expectedBytes, result);
  }

  @ParameterizedTest
  @MethodSource("provideRemittanceInformation")
  void testMapDebtPositionsToEncodedPagati(String remittanceInformation, String originalRemittanceInformation, String expectedRemittanceInformation) {
    // Given
    podamFactory.getStrategy().setDefaultNumberOfCollectionElements(2);
    DebtPositionDTO debtPosition = podamFactory.manufacturePojo(DebtPositionDTO.class);
    debtPosition.getPaymentOptions()
      .stream().map(PaymentOptionDTO::getInstallments)
      .flatMap(List::stream)
      .forEach(installment -> installment.setTransfers(List.of(installment.getTransfers().getFirst())));
    InstallmentDTO installment = debtPosition.getPaymentOptions().getFirst().getInstallments().get(1);
    installment.setOriginalRemittanceInformation(originalRemittanceInformation);
    TransferDTO transferDTO = installment.getTransfers().getFirst();
    transferDTO.setRemittanceInformation(remittanceInformation);
    Organization organization = podamFactory.manufacturePojo(Organization.class);
    debtPosition.setOrganizationId(organization.getOrganizationId());
    ReceiptDTO receipt = podamFactory.manufacturePojo(ReceiptDTO.class);
    receipt.setCreditorReferenceId(installment.getIuv());

    byte[] expectedBytes = "expectedBytes".getBytes();

    Mockito.when(receiptServiceMock.getReceiptById(installment.getReceiptId(), "accessToken"))
      .thenReturn(receipt);

    Mockito.when(jaxbTransformServiceMock.marshallingAsBytes(Mockito.argThat(p -> {
      Assertions.assertNotNull(p);
      TestUtils.checkNotNullFields(p, "riferimentoDataRichiesta");
      TestUtils.checkNotNullFields(p.getDominio(), "identificativoStazioneRichiedente");
      TestUtils.checkNotNullFields(p.getEnteBeneficiario(),"codiceUnitOperBeneficiario",
        "denomUnitOperBeneficiario",
        "indirizzoBeneficiario",
        "civicoBeneficiario",
        "capBeneficiario",
        "localitaBeneficiario",
        "provinciaBeneficiario",
        "nazioneBeneficiario");
      TestUtils.checkNotNullFields(p.getEnteBeneficiario().getIdentificativoUnivocoBeneficiario());
      TestUtils.checkNotNullFields(p.getIstitutoAttestante(),"codiceUnitOperAttestante",
        "denomUnitOperAttestante",
        "indirizzoAttestante",
        "civicoAttestante",
        "capAttestante",
        "localitaAttestante",
        "provinciaAttestante",
        "nazioneAttestante");
      TestUtils.checkNotNullFields(p.getIstitutoAttestante().getIdentificativoUnivocoAttestante());
      TestUtils.checkNotNullFields(p.getSoggettoPagatore());
      TestUtils.checkNotNullFields(p.getSoggettoPagatore().getIdentificativoUnivocoPagatore());
      TestUtils.checkNotNullFields(p.getDatiPagamento());
      p.getDatiPagamento().getDatiSingoloPagamentos().forEach(TestUtils::checkNotNullFields);

      Assertions.assertEquals(installment.getIuv(), p.getDatiPagamento().getIdentificativoUnivocoVersamento());
      Assertions.assertEquals(expectedRemittanceInformation,
        p.getDatiPagamento().getDatiSingoloPagamentos().getFirst().getCausaleVersamento());

      return true;
    }), Mockito.eq(Pagati.class))).thenReturn(expectedBytes);

    // When
    byte[] result = pagatiMapper.mapDebtPositionsToEncodedPagati(installment, organization, "accessToken");

    // Then
    Assertions.assertEquals(expectedBytes, result);
  }

  private static Stream<Arguments> provideRemittanceInformation() {
    return Stream.of(
      Arguments.of("remittanceInformation", null, "remittanceInformation"),
      Arguments.of("remittanceInformation", "originalRemittanceInformation", "remittanceInformation"),
      Arguments.of(INSTALLMENT_REMITTANCE_INFORMATION_PLACEHOLDER +" with remittanceInformation", "originalRemittanceInformation", "originalRemittanceInformation")
    );
  }
}
