package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.util.Constants;
import it.veneto.regione.pagamenti.ente.*;
import it.veneto.regione.schemas._2012.pagamenti.ente.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

  @Mock
  private DebtPositionService debtPositionServiceMock;

  @InjectMocks
  private ValidationService validationService;

  //region: validateDebtPositionTypeOrg
  @Test
  void validateDebtPositionTypeOrg_NullDebtPositionTypeOrg_ReturnsError() {
    String debtPositionTypeOrgCode = "CODE";
    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateDebtPositionTypeOrg(null, debtPositionTypeOrgCode));

    assertEquals(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, result.getFault());
    assertEquals("Tipo dovuto non valido: CODE", result.getDescription());
  }

  @Test
  void validateDebtPositionTypeOrg_InactiveDebtPositionTypeOrg_ReturnsError() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setFlagActive(false);
    String debtPositionTypeOrgCode = "CODE";

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateDebtPositionTypeOrg(debtPositionTypeOrg, debtPositionTypeOrgCode));

    assertEquals(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO, result.getFault());
    assertEquals("Tipo dovuto non abilitato: CODE", result.getDescription());
  }

  @Test
  void validateDebtPositionTypeOrg_ValidDebtPositionTypeOrg_ReturnsNull() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setFlagActive(true);

    Assertions.assertDoesNotThrow(() -> validationService.validateDebtPositionTypeOrg(debtPositionTypeOrg, "CODE"));
  }
  //endregion

  //region: validateStamp
  @Test
  void validateStamp_NullStamp_ReturnsError() {
    String debtPositionTypeOrgCode = Constants.STAMP_DEBT_POSITION_TYPE_ORG_CODE;

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateStamp(null, debtPositionTypeOrgCode));

    assertEquals(SilFaults.PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA, result.getFault());
    assertEquals("Dati marca da bollo digitale non presenti", result.getDescription());
  }

  @Test
  void validateStamp_NullStampWithNonStampDebtPositionTypeOrgCode_ReturnsError() {
    String debtPositionTypeOrgCode = "NON_STAMP_CODE";

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateStamp(null, debtPositionTypeOrgCode));

    assertEquals(SilFaults.PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA, result.getFault());
    assertEquals("Dati marca da bollo digitale non previsti con tipo dovuto diverso da " + Constants.STAMP_DEBT_POSITION_TYPE_ORG_CODE, result.getDescription());
  }

  @Test
  void validateStamp_InvalidStampFields_ReturnsError() {
    CtDatiMarcaBolloDigitale stamp = new CtDatiMarcaBolloDigitale();
    stamp.setTipoBollo(null);
    stamp.setHashDocumento(null);
    stamp.setProvinciaResidenza(null);
    String debtPositionTypeOrgCode = Constants.STAMP_DEBT_POSITION_TYPE_ORG_CODE;

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateStamp(stamp, debtPositionTypeOrgCode));

    assertEquals(SilFaults.PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA, result.getFault());
    assertEquals("Dati marca da bollo digitale non presenti", result.getDescription());
  }

  @Test
  void validateStamp_HashDocumentoTooLong_ReturnsError() {
    CtDatiMarcaBolloDigitale stamp = new CtDatiMarcaBolloDigitale();
    stamp.setTipoBollo("Valid");
    stamp.setHashDocumento("A".repeat(73)); // Exceeds 72 characters
    stamp.setProvinciaResidenza("Valid");
    String debtPositionTypeOrgCode = Constants.STAMP_DEBT_POSITION_TYPE_ORG_CODE;

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateStamp(stamp, debtPositionTypeOrgCode));

    assertEquals(SilFaults.PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA, result.getFault());
    assertEquals("Hash documento marca da bollo digitale più lunga di 72 caratteri", result.getDescription());
  }

  @Test
  void validateStamp_ValidStampWithStampDebtPositionTypeOrgCode_ReturnsNull() {
    CtDatiMarcaBolloDigitale stamp = new CtDatiMarcaBolloDigitale();
    stamp.setTipoBollo("Valid");
    stamp.setHashDocumento("ValidHash");
    stamp.setProvinciaResidenza("Valid");
    String debtPositionTypeOrgCode = Constants.STAMP_DEBT_POSITION_TYPE_ORG_CODE;

    Assertions.assertDoesNotThrow(() -> validationService.validateStamp(stamp, debtPositionTypeOrgCode));
  }

  @Test
  void validateStamp_ValidStampWithNonStampDebtPositionTypeOrgCode_ReturnsNull() {
    CtDatiMarcaBolloDigitale stamp = new CtDatiMarcaBolloDigitale();
    stamp.setTipoBollo("Valid");
    stamp.setHashDocumento("ValidHash");
    stamp.setProvinciaResidenza("Valid");
    String debtPositionTypeOrgCode = "NON_STAMP_CODE";

    Assertions.assertDoesNotThrow(() -> validationService.validateStamp(stamp, debtPositionTypeOrgCode));
  }
  //endregion

  //region: validateIud
  @Test
  void validateIud_DuplicateIud_ReturnsError() {
    Long orgId = 1L;
    String iud = "DUPLICATE_IUD";
    String accessToken = "TOKEN";

    when(debtPositionServiceMock.countExistingInstallmentsByIudIuvNav(orgId, iud, null, null, accessToken)).thenReturn(1L);

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateIud(orgId, iud, accessToken));

    assertEquals(SilFaults.PAA_IUD_DUPLICATO, result.getFault());
    assertEquals("IUD duplicato: DUPLICATE_IUD", result.getDescription());
  }

  @Test
  void validateIud_UniqueIud_ReturnsNull() {
    Long orgId = 1L;
    String iud = "UNIQUE_IUD";
    String accessToken = "TOKEN";

    when(debtPositionServiceMock.countExistingInstallmentsByIudIuvNav(orgId, iud, null, null, accessToken)).thenReturn(0L);

    Assertions.assertDoesNotThrow(() -> validationService.validateIud(orgId, iud, accessToken));
  }
  //endregion

  //region: validatePaymentData
  @Test
  void validatePaymentData_InvalidAmount_ReturnsError() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    versamento.setImportoSingoloVersamento(BigDecimal.ZERO);
    versamento.setCausaleVersamento("Valid causale");
    versamento.setDatiSpecificiRiscossione("9/ValidData");

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validatePaymentData(versamento));

    assertEquals(SilFaults.PAA_IMPORTO_SINGOLO_VERSAMENTO_NON_VALIDO, result.getFault());
    assertEquals("Importo singolo versamento non valido: 0", result.getDescription());
  }

  @Test
  void validatePaymentData_NullAmount_ReturnsError() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    versamento.setImportoSingoloVersamento(null);
    versamento.setCausaleVersamento("Valid causale");
    versamento.setDatiSpecificiRiscossione("9/ValidData");

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validatePaymentData(versamento));

    assertEquals(SilFaults.PAA_IMPORTO_SINGOLO_VERSAMENTO_NON_VALIDO, result.getFault());
    assertEquals("Importo singolo versamento non valido: null", result.getDescription());
  }

  @Test
  void validatePaymentData_InvalidDatiSpecificiRiscossione_ReturnsError() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    versamento.setImportoSingoloVersamento(BigDecimal.TEN);
    versamento.setDatiSpecificiRiscossione("INVALID");
    versamento.setCausaleVersamento("Valid causale");

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validatePaymentData(versamento));

    assertEquals(SilFaults.PAA_DATI_SPECIFICI_RISCOSSIONE_NON_VALIDO, result.getFault());
    assertEquals("Dati specifici riscossione non validi: INVALID", result.getDescription());
  }

  @Test
  void validatePaymentData_InvalidBalanceAmount_ReturnsError() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    versamento.setImportoSingoloVersamento(BigDecimal.TEN);
    Bilancio bilancio = new Bilancio();
    CtCapitolo capitolo = new CtCapitolo();
    CtAccertamento accertamento = new CtAccertamento();
    accertamento.setImporto(BigDecimal.TWO);
    capitolo.getAccertamentos().add(accertamento);
    bilancio.getCapitolos().add(capitolo);
    versamento.setBilancio(bilancio);
    versamento.setCausaleVersamento("Valid causale");
    versamento.setDatiSpecificiRiscossione("9/ValidData");

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validatePaymentData(versamento));

    assertEquals(SilFaults.PAA_IMPORTO_BILANCIO_NON_VALIDO, result.getFault());
    assertEquals("Importo bilancio non valido", result.getDescription());
  }

  @Test
  void validatePaymentData_BlankCausale_ReturnsError() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    versamento.setImportoSingoloVersamento(BigDecimal.TEN);
    versamento.setDatiSpecificiRiscossione("9/ValidData");
    versamento.setCausaleVersamento(" ");

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validatePaymentData(versamento));

    assertEquals(SilFaults.PAA_CAUSALE_NON_PRESENTE, result.getFault());
    assertEquals("Causale versamento non presente o non valida", result.getDescription());
  }

  @Test
  void validatePaymentData_ValidData_ReturnsNull() {
    CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
    versamento.setImportoSingoloVersamento(BigDecimal.TEN);
    Bilancio bilancio = new Bilancio();
    CtCapitolo capitolo = new CtCapitolo();
    CtAccertamento accertamento = new CtAccertamento();
    accertamento.setImporto(BigDecimal.TEN);
    capitolo.getAccertamentos().add(accertamento);
    bilancio.getCapitolos().add(capitolo);
    versamento.setBilancio(bilancio);
    versamento.setCausaleVersamento("Valid causale");
    versamento.setDatiSpecificiRiscossione("9/ValidData");

    Assertions.assertDoesNotThrow(() -> validationService.validatePaymentData(versamento));
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
  void validateSecondaryDebtPositionData_InvalidPaymentMetadata_ReturnsError() {
    CtDatiVersamentoDovutiEntiSecondari secondaryTransferData = new CtDatiVersamentoDovutiEntiSecondari();
    secondaryTransferData.setCodiceFiscaleBeneficiario("12345678901");
    secondaryTransferData.setIbanAccreditoBeneficiario("IT60X0542811101000000123456");
    secondaryTransferData.setImportoSingoloVersamento(BigDecimal.TEN);
    secondaryTransferData.setDatiSpecificiRiscossione("INVALID_METADATA");

    SilFaultException result = Assertions.assertThrows(SilFaultException.class, () -> validationService.validateSecondaryDebtPositionData(secondaryTransferData, 1));

    assertEquals(SilFaults.PAA_DATI_SPECIFICI_RISCOSSIONE_NON_VALIDO, result.getFault());
    assertEquals("Dati specifici riscossione non validi: INVALID_METADATA", result.getDescription());
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
}
