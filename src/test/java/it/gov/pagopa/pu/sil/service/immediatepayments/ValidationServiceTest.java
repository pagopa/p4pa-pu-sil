package it.gov.pagopa.pu.sil.service.immediatepayments;

import static it.gov.pagopa.pu.sil.util.Constants.ORDINARY_DEBT_POSITION_ORIGINS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import it.gov.pagopa.pu.sil.connector.debtpositions.InstallmentService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.veneto.regione.pagamenti.ente.ElementoListaDovuti;
import it.veneto.regione.pagamenti.ente.ElementoListaDovutiEntiSecondari;
import it.veneto.regione.pagamenti.ente.ListaDovuti;
import it.veneto.regione.pagamenti.ente.ListaDovutiEntiSecondari;
import it.veneto.regione.pagamenti.ente.PaaSILInviaCarrelloDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiMarcaBolloDigitale;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiSingoloVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiVersamentoDovutiEntiSecondari;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

  @Mock
  private InstallmentService installmentServiceMock;

  @InjectMocks
  private ValidationService validationService;

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(installmentServiceMock);
  }

  //region: validateStamp
  @Test
  void validateStamp_NullStamp_DoesNothing() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    versamento.setDatiMarcaBolloDigitale(null);

    Assertions.assertDoesNotThrow(() -> validationService.validateStamp(versamento));
    Assertions.assertNull(versamento.getDatiMarcaBolloDigitale());
  }

  @Test
  void validateStamp_AllFieldsBlank_RemovesStamp() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    CtDatiMarcaBolloDigitale stamp = new CtDatiMarcaBolloDigitale();
    stamp.setTipoBollo(" ");
    stamp.setHashDocumento(" ");
    stamp.setProvinciaResidenza(" ");
    versamento.setDatiMarcaBolloDigitale(stamp);

    Assertions.assertDoesNotThrow(() -> validationService.validateStamp(versamento));
    Assertions.assertNull(versamento.getDatiMarcaBolloDigitale());
  }

  @Test
  void validateStamp_HashDocumentoTooShort_Throws() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    CtDatiMarcaBolloDigitale stamp = new CtDatiMarcaBolloDigitale();
    stamp.setHashDocumento("abc"); // too short
    stamp.setProvinciaResidenza("VE");
    stamp.setTipoBollo("01");
    versamento.setDatiMarcaBolloDigitale(stamp);

    SilFaultException ex = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateStamp(versamento));
    assertEquals(SilFaults.PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA, ex.getFault());
    Assertions.assertTrue(ex.getDescription().contains("hash documento"));
  }

  @Test
  void validateStamp_HashDocumentoTooLong_Throws() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    CtDatiMarcaBolloDigitale stamp = new CtDatiMarcaBolloDigitale();
    stamp.setHashDocumento("a".repeat(73)); // too long
    stamp.setProvinciaResidenza("VE");
    stamp.setTipoBollo("01");
    versamento.setDatiMarcaBolloDigitale(stamp);

    SilFaultException ex = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateStamp(versamento));
    assertEquals(SilFaults.PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA, ex.getFault());
    Assertions.assertTrue(ex.getDescription().contains("hash documento"));
  }

  @Test
  void validateStamp_ProvinciaResidenzaWrongLength_Throws() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    CtDatiMarcaBolloDigitale stamp = new CtDatiMarcaBolloDigitale();
    stamp.setHashDocumento("abcd");
    stamp.setProvinciaResidenza("V"); // too short
    stamp.setTipoBollo("01");
    versamento.setDatiMarcaBolloDigitale(stamp);

    SilFaultException ex = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateStamp(versamento));
    assertEquals(SilFaults.PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA, ex.getFault());
    Assertions.assertTrue(ex.getDescription().contains("provincia residenza"));
  }

  @Test
  void validateStamp_TipoBolloWrongLength_Throws() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    CtDatiMarcaBolloDigitale stamp = new CtDatiMarcaBolloDigitale();
    stamp.setHashDocumento("abcd");
    stamp.setProvinciaResidenza("VE");
    stamp.setTipoBollo("1"); // too short
    versamento.setDatiMarcaBolloDigitale(stamp);

    SilFaultException ex = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateStamp(versamento));
    assertEquals(SilFaults.PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA, ex.getFault());
    Assertions.assertTrue(ex.getDescription().contains("tipo bollo"));
  }

  @Test
  void validateStamp_ValidStamp_DoesNothing() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    CtDatiMarcaBolloDigitale stamp = new CtDatiMarcaBolloDigitale();
    stamp.setHashDocumento("abcd");
    stamp.setProvinciaResidenza("VE");
    stamp.setTipoBollo("01");
    versamento.setDatiMarcaBolloDigitale(stamp);

    Assertions.assertDoesNotThrow(() -> validationService.validateStamp(versamento));
    Assertions.assertNotNull(versamento.getDatiMarcaBolloDigitale());
  }
  //endregion

  //region: validateIud
  @Test
  void validateIud_DuplicateIud_ReturnsError() {
    Long orgId = 1L;
    String iud = "DUPLICATE_IUD";
    String accessToken = "TOKEN";

    when(installmentServiceMock.isInstallmentExistsByIudIuvNav(orgId, iud, null, null, ORDINARY_DEBT_POSITION_ORIGINS, accessToken)).thenReturn(Boolean.TRUE);

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateIud(orgId, iud, accessToken));

    assertEquals(SilFaults.PAA_IUD_DUPLICATO, result.getFault());
    assertEquals("IUD duplicato: DUPLICATE_IUD", result.getDescription());
  }

  @Test
  void validateIud_UniqueIud_ReturnsNull() {
    Long orgId = 1L;
    String iud = "UNIQUE_IUD";
    String accessToken = "TOKEN";

    when(installmentServiceMock.isInstallmentExistsByIudIuvNav(orgId, iud, null, null, ORDINARY_DEBT_POSITION_ORIGINS, accessToken)).thenReturn(Boolean.FALSE);

    Assertions.assertDoesNotThrow(() -> validationService.validateIud(orgId, iud, accessToken));
  }
  //endregion

  //region: validatePrimaryDebtPositionOrganization
  @Test
  void validatePrimaryDebtPositionOrganization_NullListaDovuti_ReturnsError() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    request.setListaDovuti(null);

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validatePrimaryDebtPositionOrganization(request, "ORG_CODE"));

    assertEquals(SilFaults.PAA_SYSTEM_ERROR, result.getFault());
    assertEquals("Dovuti non presenti", result.getDescription());
  }

  @Test
  void validatePrimaryDebtPositionOrganization_EmptyListaDovuti_ReturnsError() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    request.setListaDovuti(new ListaDovuti());

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validatePrimaryDebtPositionOrganization(request, "ORG_CODE"));

    assertEquals(SilFaults.PAA_SYSTEM_ERROR, result.getFault());
    assertEquals("Dovuti non presenti", result.getDescription());
  }

  @Test
  void validatePrimaryDebtPositionOrganization_InvalidCodIpaEnte_ReturnsError() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    request.setListaDovuti(new ListaDovuti());
    ElementoListaDovuti elementoListaDovuti = new ElementoListaDovuti();
    elementoListaDovuti.setCodIpaEnte("INVALID_ORG_CODE");
    request.getListaDovuti().getElementoListaDovutis().add(elementoListaDovuti);

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validatePrimaryDebtPositionOrganization(request, "ORG_CODE"));

    assertEquals(SilFaults.PAA_ENTE_NON_VALIDO, result.getFault());
    assertEquals("L'inserimento di dovuti per enti diversi dal chiamante è deprecato", result.getDescription());
  }

  @Test
  void validatePrimaryDebtPositionOrganization_ValidCodIpaEnte_ReturnsNull() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    request.setListaDovuti(new ListaDovuti());
    ElementoListaDovuti elementoListaDovuti = new ElementoListaDovuti();
    elementoListaDovuti.setCodIpaEnte("ORG_CODE");
    request.getListaDovuti().getElementoListaDovutis().add(elementoListaDovuti);

    Assertions.assertDoesNotThrow(() -> validationService.validatePrimaryDebtPositionOrganization(request, "ORG_CODE"));
  }
  //endregion

  //region: validateSecondaryDebtPositionCount
  @Test
  void validateSecondaryDebtPositionCount_NoSecondaryDebtPositions_ReturnsNull() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    request.setListaDovutiEntiSecondari(null);

    Assertions.assertDoesNotThrow(() -> validationService.validateSecondaryDebtPositionCount(request, 1));
  }

  @Test
  void validateSecondaryDebtPositionCount_MultiplePrimaryDebtPositions_ReturnsError() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    ListaDovutiEntiSecondari listaDovutiEntiSecondari = new ListaDovutiEntiSecondari();
    listaDovutiEntiSecondari.getElementoListaDovutiEntiSecondaris().add(new ElementoListaDovutiEntiSecondari());
    request.setListaDovutiEntiSecondari(listaDovutiEntiSecondari);

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateSecondaryDebtPositionCount(request, 2));

    assertEquals(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_MULTIBENEFICIARI, result.getFault());
    assertEquals("Non è possibile inserire un pagamento multibeneficiario se sono presenti più di un dovuto", result.getDescription());
  }

  @Test
  void validateSecondaryDebtPositionCount_MultipleSecondaryDebtPositions_ReturnsError() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    ListaDovutiEntiSecondari listaDovutiEntiSecondari = new ListaDovutiEntiSecondari();
    listaDovutiEntiSecondari.getElementoListaDovutiEntiSecondaris().add(new ElementoListaDovutiEntiSecondari());
    listaDovutiEntiSecondari.getElementoListaDovutiEntiSecondaris().add(new ElementoListaDovutiEntiSecondari());
    request.setListaDovutiEntiSecondari(listaDovutiEntiSecondari);

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateSecondaryDebtPositionCount(request, 1));

    assertEquals(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_MULTIBENEFICIARI, result.getFault());
    assertEquals("Non è possibile inserire pagamenti multibeneficiario con più di un dovuto secondario", result.getDescription());
  }

  @Test
  void validateSecondaryDebtPositionCount_ValidSecondaryDebtPositions_ReturnsNull() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    ListaDovutiEntiSecondari listaDovutiEntiSecondari = new ListaDovutiEntiSecondari();
    listaDovutiEntiSecondari.getElementoListaDovutiEntiSecondaris().add(new ElementoListaDovutiEntiSecondari());
    request.setListaDovutiEntiSecondari(listaDovutiEntiSecondari);

    Assertions.assertDoesNotThrow(() -> validationService.validateSecondaryDebtPositionCount(request, 1));
  }
  //endregion

  //region: validateSecondaryDebtPositionData
  @Test
  void validateSecondaryDebtPositionData_MultiplePrimaryDebtPositions_ReturnsError() {
    CtDatiVersamentoDovutiEntiSecondari secondaryTransferData = new CtDatiVersamentoDovutiEntiSecondari();
    secondaryTransferData.setCodiceFiscaleBeneficiario("12345678901");
    secondaryTransferData.setIbanAccreditoBeneficiario("IT60X0542811101000000123456");
    secondaryTransferData.setImportoSingoloVersamento(BigDecimal.TEN);
    secondaryTransferData.setDatiSpecificiRiscossione("9/1234567IM/ValidData");

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateSecondaryDebtPositionData(secondaryTransferData, 2));

    assertEquals(SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_MULTIBENEFICIARI, result.getFault());
    assertEquals("Non è possibile inserire pagamenti multibeneficiario con più di un dovuto", result.getDescription());
  }

  @Test
  void validateSecondaryDebtPositionData_InvalidFiscalCode_ReturnsError() {
    CtDatiVersamentoDovutiEntiSecondari secondaryTransferData = new CtDatiVersamentoDovutiEntiSecondari();
    secondaryTransferData.setCodiceFiscaleBeneficiario("INVALID_CF");
    secondaryTransferData.setIbanAccreditoBeneficiario("IT60X0542811101000000123456");
    secondaryTransferData.setImportoSingoloVersamento(BigDecimal.TEN);
    secondaryTransferData.setDatiSpecificiRiscossione("9/1234567IM/ValidData");

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateSecondaryDebtPositionData(secondaryTransferData, 1));

    assertEquals(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, result.getFault());
    assertEquals("Codice fiscale ente secondario non valido: INVALID_CF", result.getDescription());
  }

  @Test
  void validateSecondaryDebtPositionData_InvalidIban_ReturnsError() {
    CtDatiVersamentoDovutiEntiSecondari secondaryTransferData = new CtDatiVersamentoDovutiEntiSecondari();
    secondaryTransferData.setCodiceFiscaleBeneficiario("12345678901");
    secondaryTransferData.setIbanAccreditoBeneficiario("INVALID_IBAN");
    secondaryTransferData.setImportoSingoloVersamento(BigDecimal.TEN);
    secondaryTransferData.setDatiSpecificiRiscossione("9/1234567IM/ValidData");

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateSecondaryDebtPositionData(secondaryTransferData, 1));

    assertEquals(SilFaults.PAA_ENTE_SECONDARIO_NON_VALIDO, result.getFault());
    assertEquals("IBAN accredito Ente secondario non valido [INVALID_IBAN]", result.getDescription());
  }

  @Test
  void validateSecondaryDebtPositionData_InvalidAmount_ReturnsError() {
    CtDatiVersamentoDovutiEntiSecondari secondaryTransferData = new CtDatiVersamentoDovutiEntiSecondari();
    secondaryTransferData.setCodiceFiscaleBeneficiario("12345678901");
    secondaryTransferData.setIbanAccreditoBeneficiario("IT60X0542811101000000123456");
    secondaryTransferData.setImportoSingoloVersamento(BigDecimal.ZERO);
    secondaryTransferData.setDatiSpecificiRiscossione("9/1234567IM/ValidData");

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateSecondaryDebtPositionData(secondaryTransferData, 1));

    assertEquals(SilFaults.PAA_IMPORTO_SINGOLO_VERSAMENTO_NON_VALIDO, result.getFault());
    assertEquals("Importo singolo versamento non valido: 0", result.getDescription());
  }

  @Test
  void validateSecondaryDebtPositionData_ValidData_ReturnsNull() {
    CtDatiVersamentoDovutiEntiSecondari secondaryTransferData = new CtDatiVersamentoDovutiEntiSecondari();
    secondaryTransferData.setCodiceFiscaleBeneficiario("12345678901");
    secondaryTransferData.setIbanAccreditoBeneficiario("IT60X0542811101000000123456");
    secondaryTransferData.setImportoSingoloVersamento(BigDecimal.TEN);
    secondaryTransferData.setDatiSpecificiRiscossione("9/1234567IM/ValidData");

    Assertions.assertDoesNotThrow(() -> validationService.validateSecondaryDebtPositionData(secondaryTransferData, 1));
  }
  //endregion

  //region: validateCartSize
  @ParameterizedTest
  @CsvSource({
    "10, PAA_LIMITE_MASSIMO_DOVUTI_CARRELLO, 'Numero massimo dovuti nel carrello superato: 10/5'",
    "0, PAA_XML_NON_VALIDO, 'Nessun dovuto presente'",
    "1, , " // valid case, should not throw
  })
  void validateCartSize_Parametrized(int size, String expectedFault, String expectedDescription) {
    if (expectedFault != null) {
      SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateCartSize(size));
      assertEquals(SilFaults.valueOf(expectedFault), result.getFault());
      assertEquals(expectedDescription, result.getDescription());
    } else {
      Assertions.assertDoesNotThrow(() -> validationService.validateCartSize(size));
    }
  }
  //endregion
}
