package it.gov.pagopa.pu.sil.util;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.veneto.regione.schemas._2012.pagamenti.ente.Bilancio;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtAccertamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.co.jemos.podam.api.PodamFactory;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

  PodamFactory podamFactory = TestUtils.getPodamFactory();

  @Test
  void isValidEmail_ValidEmail_ReturnsTrue() {
    assertTrue(ValidationUtils.isValidEmail("test@example.com"));
  }

  @ParameterizedTest
  @ValueSource(strings = {" ", "invalid"})
  @NullAndEmptySource
  void isValidEmail_InvalidEmail_ReturnsFalse(String input) {
    assertFalse(ValidationUtils.isValidEmail(input));
  }

  @Test
  void isValidISOCountry_ValidCountryCode_ReturnsTrue() {
    assertTrue(ValidationUtils.isValidISOCountry("IT"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"INVALID"})
  @NullAndEmptySource
  void isValidISOCountry_InvalidCountryCode_ReturnsFalse(String input) {
    assertFalse(ValidationUtils.isValidISOCountry(input));
  }

  @Test
  void isValidPostalCode_ValidPostalCodeIT_ReturnsTrue() {
    assertTrue(ValidationUtils.isValidPostalCode("12345", "IT"));
  }

  @Test
  void isValidPostalCode_ValidPostalCode_ReturnsTrue() {
    assertTrue(ValidationUtils.isValidPostalCode("12345ABCD", "FR"));
  }

  @Test
  void isValidPostalCode_InvalidPostalCodeIT_ReturnsFalse() {
    assertFalse(ValidationUtils.isValidPostalCode("ABCDE", "IT"));
  }

  @Test
  void isValidPostalCode_InvalidPostalCode_ReturnsFalse() {
    assertFalse(ValidationUtils.isValidPostalCode("ABCDEDLFJ-$MCO$NDKL", "FR"));
  }

  @Test
  void isValidPostalCode_NullPostalCode_ReturnsFalse() {
    assertFalse(ValidationUtils.isValidPostalCode(null, "IT"));
  }

  @Test
  void isValidCivic_ValidCivicNumber_ReturnsTrue() {
    assertTrue(ValidationUtils.isValidCivic("123"));
  }

  @Test
  void isValidCivic_InvalidCivicNumber_ReturnsFalse() {
    assertFalse(ValidationUtils.isValidCivic("INVA?LID"));
  }

  @Test
  void isValidCivic_NullCivicNumber_ReturnsFalse() {
    assertFalse(ValidationUtils.isValidCivic(null));
  }

  @Test
  void isValidProvince_ValidProvince_ReturnsTrue() {
    assertTrue(ValidationUtils.isValidProvince("RM"));
  }

  @Test
  void isValidProvince_InvalidProvince_ReturnsFalse() {
    assertFalse(ValidationUtils.isValidProvince("INVALID"));
  }

  @Test
  void isValidProvince_NullProvince_ReturnsFalse() {
    assertFalse(ValidationUtils.isValidProvince(null));
  }

  @Test
  void verifyBalanceAmount_NullBalance_ReturnsTrue() {
    assertTrue(ValidationUtils.verifyBalanceAmount(null, BigDecimal.TEN));
  }

  @Test
  void verifyBalanceAmount_EmptyCapitoloList_ReturnsTrue() {
    Bilancio balance = new Bilancio();

    assertTrue(ValidationUtils.verifyBalanceAmount(balance, BigDecimal.ZERO));
  }

  @ParameterizedTest
  @ValueSource(strings = {"matching", "nonMatching"})
  void verifyBalanceAmount_MatchingAmount(String testType) {
    Bilancio balance = podamFactory.manufacturePojo(Bilancio.class);
    BigDecimal expectedResult = balance
      .getCapitolos().stream()
      .flatMap(c -> c.getAccertamentos().stream().map(CtAccertamento::getImporto))
        .reduce(BigDecimal.ZERO, BigDecimal::add)
          .add(testType.equals("matching")?BigDecimal.ZERO:BigDecimal.TEN);

    assertEquals(testType.equals("matching"), ValidationUtils.verifyBalanceAmount(balance, expectedResult));
  }

  @Test
  void verifyValidAnonymousDebtor_AnonymousFiscalCode_ReturnsTrue() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(true);

    PersonDTO personDTO = new PersonDTO();
    personDTO.setFiscalCode(Constants.ANONYMOUS_FISCAL_CODE);
    personDTO.setEntityType(PersonEntityType.F);

    assertTrue(ValidationUtils.verifyValidAnonymousDebtor(debtPositionTypeOrg, personDTO));
  }

  @Test
  void verifyValidAnonymousDebtor_NonAnonymousFiscalCode_ReturnsTrue() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(false);

    PersonDTO personDTO = new PersonDTO();
    personDTO.setFiscalCode("RSSMRA85M01H501Z");
    personDTO.setEntityType(PersonEntityType.F);

    assertTrue(ValidationUtils.verifyValidAnonymousDebtor(debtPositionTypeOrg, personDTO));
  }

  @Test
  void verifyValidAnonymousDebtor_InvalidAnonymousFiscalCode_ReturnsFalse() {
    DebtPositionTypeOrg debtPositionTypeOrg = new DebtPositionTypeOrg();
    debtPositionTypeOrg.setFlagAnonymousFiscalCode(true);

    PersonDTO personDTO = new PersonDTO();
    personDTO.setFiscalCode(Constants.ANONYMOUS_FISCAL_CODE);
    personDTO.setEntityType(PersonEntityType.G);

    assertFalse(ValidationUtils.verifyValidAnonymousDebtor(debtPositionTypeOrg, personDTO));
  }

  @Test
  void verifyValidAnonymousDebtor_NullDebtPositionTypeOrg_ReturnsFalse() {
    PersonDTO personDTO = new PersonDTO();
    personDTO.setFiscalCode("RSSMRA85M01H501Z");
    personDTO.setEntityType(PersonEntityType.F);

    assertFalse(ValidationUtils.verifyValidAnonymousDebtor(null, personDTO));
  }

  @Test
  void isValidFiscalCodeNaturalPerson_ValidFiscalCode_ReturnsTrue() {
    assertTrue(ValidationUtils.isValidFiscalCodeNaturalPerson("RSSMRA85M01H501Z"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"INVALID", "RSSMRA85M01H501Z1", "AFFMRA85M01H501Z", "0FFMRA85M01H501Z"})
  @NullAndEmptySource
  void isValidFiscalCodeNaturalPerson_InvalidFiscalCode_ReturnsFalse(String fiscalCode) {
    assertFalse(ValidationUtils.isValidFiscalCodeNaturalPerson(fiscalCode));
  }

  @Test
  void isValidFiscalCodeLegalEntity_ValidFiscalCode_ReturnsTrue() {
    assertTrue(ValidationUtils.isValidFiscalCodeLegalEntity("12345678901"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"INVALID", "123456789012", "A2345678901"})
  @NullAndEmptySource
  void isValidFiscalCodeLegalEntity_InvalidFiscalCode_ReturnsFalse(String fiscalCode) {
    assertFalse(ValidationUtils.isValidFiscalCodeLegalEntity(fiscalCode));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0/ValidData", "1/AnotherValidData", "9/Valid123", "2/Valid-Data"})
  void isValidLegacyPaymentMetadata_ValidInputs_ReturnsTrue(String input) {
    assertTrue(ValidationUtils.isValidLegacyPaymentMetadata(input));
  }

  @ParameterizedTest
  @ValueSource(strings = {"InvalidData", "3/TooShort", "4/", "5/ThisDataIsWayTooLongToBeValidBecauseItExceedsTheMaximumAllowedLengthOf138Characters12345678901234567890123456789012345678901234567890123456789012345678901234567890"})
  @NullAndEmptySource
  void isValidLegacyPaymentMetadata_InvalidInputs_ReturnsFalse(String input) {
    assertFalse(ValidationUtils.isValidLegacyPaymentMetadata(input));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0/1234534IM/", "1/2435236SA/an b", "9/1646246AP/long/long", "2/9079248TS/."})
  void isValidLegacyPaymentMetadataSecondary_ValidInputs_ReturnsTrue(String input) {
    assertTrue(ValidationUtils.isValidLegacyPaymentMetadataSecondary(input));
  }

  @ParameterizedTest
  @ValueSource(strings = {"InvalidData", "0/1234534AA/", "0/1234534IM", "0/123453IM/too_short", "0/123453FIM/invalid taxonomy", "0/1234534IM/ThisDataIsWayTooLongToBeValidBecauseItExceedsTheMaximumAllowedLengthOf138Characters12345678901234567890123456789012345678901234567890123456789012345678901234567890"})
  @NullAndEmptySource
  void isValidLegacyPaymentMetadataSecondary_InvalidInputs_ReturnsFalse(String input) {
    assertFalse(ValidationUtils.isValidLegacyPaymentMetadataSecondary(input));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0/1234534IM/", "1/2435236SA/an b", "9/1646246AP/long/long", "2/9079248TS/."})
  void getTransferCategoryFromLegacyPaymentMetadataSecondary_ValidInputs_ReturnsCategory(String input) {
    String category = ValidationUtils.getTransferCategoryFromLegacyPaymentMetadataSecondary(input);
    assertNotNull(category);
    assertEquals(input.substring(2,11), category);
  }

  @ParameterizedTest
  @ValueSource(strings = {"InvalidData", "0/1234534AA/", "0/1234534IM", "0/123453IM/too_short", "0/123453FIM/invalid taxonomy", "0/1234534IM/ThisDataIsWayTooLongToBeValidBecauseItExceedsTheMaximumAllowedLengthOf138Characters12345678901234567890123456789012345678901234567890123456789012345678901234567890"})
  @NullAndEmptySource
  void getTransferCategoryFromLegacyPaymentMetadataSecondary_InvalidInputs_ReturnsNull(String input) {
    assertNull(ValidationUtils.getTransferCategoryFromLegacyPaymentMetadataSecondary(input));
  }

  @ParameterizedTest
  @ValueSource(strings = {"IT60X0542811101000000123456", "IT23A0200801002000301234567"})
  void isValidIban_ValidInputs_ReturnsTrue(String input) {
    assertTrue(ValidationUtils.isValidIban(input));
  }

  @ParameterizedTest
  @ValueSource(strings = {"IT60X054281110100000012345", "FR1420051010050500043M02606", "IT60X0542811101000000123435G"})
  @NullAndEmptySource
  void isValidIban_InvalidInputs_ReturnsFalse(String input) {
    assertFalse(ValidationUtils.isValidIban(input));
  }

  @ParameterizedTest
  @ValueSource(strings = {"http://example.com", "https://example.com/path?query=1", "ftp://ftp.example.com"})
  void isValidUri_ValidUri_ReturnsTrue(String uri) {
    assertTrue(ValidationUtils.isValidUri(uri));
  }

  @ParameterizedTest
  @ValueSource(strings = {"http://", "://missing.scheme.com"})
  @NullAndEmptySource
  void isValidUri_InvalidUri_ReturnsFalse(String uri) {
    assertFalse(ValidationUtils.isValidUri(uri));
  }

  @ParameterizedTest
  @MethodSource("verifyExclusivePresenceProvider")
  void testVerifyExclusivePresence(Object obj1, Object obj2, boolean expected) {
    boolean result = ValidationUtils.verifyExclusivePresence(obj1, obj2);
    assertEquals(expected, result);
  }

  private static Stream<Arguments> verifyExclusivePresenceProvider() {
    return Stream.of(
      Arguments.of(new Object(), null, true),
      Arguments.of(null, new Object(), true),
      Arguments.of(null, null, false),
      Arguments.of(new Object(), new Object(), false)
    );
  }

}
