package it.gov.pagopa.pu.sil.service.inbound.payments.immediatepayments.soap;

import it.gov.pagopa.pu.sil.connector.debtpositions.InstallmentService;
import it.gov.pagopa.pu.sil.exception.common.InvalidValueException;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import it.veneto.regione.pagamenti.ente.*;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiMarcaBolloDigitale;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiSingoloVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiVersamentoDovutiEntiSecondari;
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

import java.math.BigDecimal;

import static it.gov.pagopa.pu.sil.util.Constants.ORDINARY_DEBT_POSITION_ORIGINS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

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

    InvalidValueException ex = Assertions.assertThrows(InvalidValueException.class, () -> validationService.validateStamp(versamento));
    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_STAMP, ex.getCode());
    Assertions.assertTrue(ex.getSilFaultCustomMessage().contains("hash documento"));
  }

  @Test
  void validateStamp_HashDocumentoTooLong_Throws() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    CtDatiMarcaBolloDigitale stamp = new CtDatiMarcaBolloDigitale();
    stamp.setHashDocumento("a".repeat(73)); // too long
    stamp.setProvinciaResidenza("VE");
    stamp.setTipoBollo("01");
    versamento.setDatiMarcaBolloDigitale(stamp);

    InvalidValueException ex = Assertions.assertThrows(InvalidValueException.class, () -> validationService.validateStamp(versamento));
    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_STAMP, ex.getCode());
    Assertions.assertTrue(ex.getSilFaultCustomMessage().contains("hash documento"));
  }

  @Test
  void validateStamp_ProvinciaResidenzaWrongLength_Throws() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    CtDatiMarcaBolloDigitale stamp = new CtDatiMarcaBolloDigitale();
    stamp.setHashDocumento("abcd");
    stamp.setProvinciaResidenza("V"); // too short
    stamp.setTipoBollo("01");
    versamento.setDatiMarcaBolloDigitale(stamp);

    InvalidValueException ex = Assertions.assertThrows(InvalidValueException.class, () -> validationService.validateStamp(versamento));
    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_STAMP, ex.getCode());
    Assertions.assertTrue(ex.getSilFaultCustomMessage().contains("provincia residenza"));
  }

  @Test
  void validateStamp_TipoBolloWrongLength_Throws() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    CtDatiMarcaBolloDigitale stamp = new CtDatiMarcaBolloDigitale();
    stamp.setHashDocumento("abcd");
    stamp.setProvinciaResidenza("VE");
    stamp.setTipoBollo("1"); // too short
    versamento.setDatiMarcaBolloDigitale(stamp);

    InvalidValueException ex = Assertions.assertThrows(InvalidValueException.class, () -> validationService.validateStamp(versamento));
    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_STAMP, ex.getCode());
    Assertions.assertTrue(ex.getSilFaultCustomMessage().contains("tipo bollo"));
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

    InvalidValueException result = Assertions.assertThrows(InvalidValueException.class, () -> validationService.validateIud(orgId, iud, accessToken));

    assertEquals( ErrorCodeConstants.ERROR_CODE_DUPLICATED_IUD, result.getCode());
    assertEquals("IUD duplicato: DUPLICATE_IUD", result.getSilFaultCustomMessage());
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

    InvalidValueException result = Assertions.assertThrows(InvalidValueException.class, () -> validationService.validatePrimaryDebtPositionOrganization(request, "ORG_CODE"));

    assertEquals(ErrorCodeConstants.ERROR_CODE_SYSTEM_ERROR, result.getCode());
    assertEquals("Dovuti non presenti", result.getSilFaultCustomMessage());
  }

  @Test
  void validatePrimaryDebtPositionOrganization_EmptyListaDovuti_ReturnsError() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    request.setListaDovuti(new ListaDovuti());

    InvalidValueException result = Assertions.assertThrows(InvalidValueException.class, () -> validationService.validatePrimaryDebtPositionOrganization(request, "ORG_CODE"));

    assertEquals(  ErrorCodeConstants.ERROR_CODE_SYSTEM_ERROR, result.getCode());
    assertEquals("Dovuti non presenti", result.getSilFaultCustomMessage());
  }

  @Test
  void validatePrimaryDebtPositionOrganization_InvalidCodIpaEnte_ReturnsError() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    request.setListaDovuti(new ListaDovuti());
    ElementoListaDovuti elementoListaDovuti = new ElementoListaDovuti();
    elementoListaDovuti.setCodIpaEnte("INVALID_ORG_CODE");
    request.getListaDovuti().getElementoListaDovutis().add(elementoListaDovuti);

    InvalidValueException result = Assertions.assertThrows(InvalidValueException.class, () -> validationService.validatePrimaryDebtPositionOrganization(request, "ORG_CODE"));

    assertEquals( ErrorCodeConstants.ERROR_CODE_INVALID_ORGANIZATION, result.getCode());
    assertEquals("L'inserimento di dovuti per enti diversi dal chiamante è deprecato", result.getSilFaultCustomMessage());
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

    InvalidValueException result = Assertions.assertThrows(InvalidValueException.class, () -> validationService.validateSecondaryDebtPositionCount(request, 2));

    assertEquals(ErrorCodeConstants.ERROR_CODE_MULTIBENEFICIARY_THRESHOLD, result.getCode());
    assertEquals("Non è possibile inserire un pagamento multibeneficiario se sono presenti più di un dovuto", result.getSilFaultCustomMessage());
  }

  @Test
  void validateSecondaryDebtPositionCount_MultipleSecondaryDebtPositions_ReturnsError() {
    PaaSILInviaCarrelloDovuti request = new PaaSILInviaCarrelloDovuti();
    ListaDovutiEntiSecondari listaDovutiEntiSecondari = new ListaDovutiEntiSecondari();
    listaDovutiEntiSecondari.getElementoListaDovutiEntiSecondaris().add(new ElementoListaDovutiEntiSecondari());
    listaDovutiEntiSecondari.getElementoListaDovutiEntiSecondaris().add(new ElementoListaDovutiEntiSecondari());
    request.setListaDovutiEntiSecondari(listaDovutiEntiSecondari);

    InvalidValueException result = Assertions.assertThrows(InvalidValueException.class, () -> validationService.validateSecondaryDebtPositionCount(request, 1));

    assertEquals(ErrorCodeConstants.ERROR_CODE_MULTIBENEFICIARY_THRESHOLD, result.getCode());
    assertEquals("Non è possibile inserire pagamenti multibeneficiario con più di un dovuto secondario", result.getSilFaultCustomMessage());
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

    InvalidValueException result = Assertions.assertThrows(InvalidValueException.class, () -> validationService.validateSecondaryDebtPositionData(secondaryTransferData, 2));

    assertEquals(ErrorCodeConstants.ERROR_CODE_MULTIBENEFICIARY_THRESHOLD, result.getCode());
    assertEquals("Non è possibile inserire pagamenti multibeneficiario con più di un dovuto", result.getSilFaultCustomMessage());
  }

  @Test
  void validateSecondaryDebtPositionData_InvalidFiscalCode_ReturnsError() {
    CtDatiVersamentoDovutiEntiSecondari secondaryTransferData = new CtDatiVersamentoDovutiEntiSecondari();
    secondaryTransferData.setCodiceFiscaleBeneficiario("INVALID_CF");
    secondaryTransferData.setIbanAccreditoBeneficiario("IT60X0542811101000000123456");
    secondaryTransferData.setImportoSingoloVersamento(BigDecimal.TEN);
    secondaryTransferData.setDatiSpecificiRiscossione("9/1234567IM/ValidData");

    InvalidValueException result = Assertions.assertThrows(InvalidValueException.class, () -> validationService.validateSecondaryDebtPositionData(secondaryTransferData, 1));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_VAT_CODE, result.getCode());
    assertEquals("Codice fiscale ente secondario non valido: INVALID_CF", result.getSilFaultCustomMessage());
  }

  @Test
  void validateSecondaryDebtPositionData_InvalidIban_ReturnsError() {
    CtDatiVersamentoDovutiEntiSecondari secondaryTransferData = new CtDatiVersamentoDovutiEntiSecondari();
    secondaryTransferData.setCodiceFiscaleBeneficiario("12345678901");
    secondaryTransferData.setIbanAccreditoBeneficiario("INVALID_IBAN");
    secondaryTransferData.setImportoSingoloVersamento(BigDecimal.TEN);
    secondaryTransferData.setDatiSpecificiRiscossione("9/1234567IM/ValidData");

    InvalidValueException result = Assertions.assertThrows(InvalidValueException.class, () -> validationService.validateSecondaryDebtPositionData(secondaryTransferData, 1));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_IBAN, result.getCode());
    assertEquals("IBAN accredito Ente secondario non valido [INVALID_IBAN]", result.getSilFaultCustomMessage());
  }

  @Test
  void validateSecondaryDebtPositionData_InvalidAmount_ReturnsError() {
    CtDatiVersamentoDovutiEntiSecondari secondaryTransferData = new CtDatiVersamentoDovutiEntiSecondari();
    secondaryTransferData.setCodiceFiscaleBeneficiario("12345678901");
    secondaryTransferData.setIbanAccreditoBeneficiario("IT60X0542811101000000123456");
    secondaryTransferData.setImportoSingoloVersamento(BigDecimal.ZERO);
    secondaryTransferData.setDatiSpecificiRiscossione("9/1234567IM/ValidData");

    InvalidValueException result = Assertions.assertThrows(InvalidValueException.class, () -> validationService.validateSecondaryDebtPositionData(secondaryTransferData, 1));

    assertEquals(ErrorCodeConstants.ERROR_CODE_INVALID_AMOUNT, result.getCode());
    assertEquals("Importo singolo versamento non valido: 0", result.getSilFaultCustomMessage());
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
    "10, 'Numero massimo dovuti nel carrello superato: 10/5'",
    "0, 'Nessun dovuto presente'",
    "1, " // valid case, should not throw
  })
  void validateCartSize_Parametrized(int size, String expectedDescription) {
    if (expectedDescription != null) {
      InvalidValueException result = Assertions.assertThrows(InvalidValueException.class, () -> validationService.validateCartSize(size));
      assertEquals(expectedDescription, result.getSilFaultCustomMessage());
    } else {
      Assertions.assertDoesNotThrow(() -> validationService.validateCartSize(size));
    }
  }
  //endregion
}
