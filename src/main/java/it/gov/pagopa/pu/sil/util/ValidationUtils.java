package it.gov.pagopa.pu.sil.util;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.EntityTypeEnum;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.veneto.regione.schemas._2012.pagamenti.ente.Bilancio;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtAccertamento;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidationUtils {
  private static final Pattern LEGACY_PAYMENT_METADATA_PATTERN = Pattern.compile("^[0129]/\\S{3,138}$");
  private static final Pattern LEGACY_PAYMENT_METADATA_SECONDARY_PATTERN = Pattern.compile("^[0129]/(\\d{7}(?:IM|TS|SP|SA|AP))/.{0,128}$");

  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");
  private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());
  private static final Pattern PROVINCE_PATTERN = Pattern.compile("^[A-Z]{2}$");
  private static final Pattern POSTAL_CODE_IT_PATTERN = Pattern.compile("^\\d{5}$");
  private static final Pattern POSTAL_CODE_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,16}$");
  private static final Pattern CIVIC_PATTERN = Pattern.compile("^[a-z A-Z0-9.,()/'&]{1,16}$");
  private static final Pattern FISCAL_CODE_NATURAL_PERSON_PATTERN = Pattern.compile(
    "^(?:[A-Z][AEIOUX][AEIOUX]|[B-DF-HJ-NP-TV-Z]{2}[A-Z]){2}(?:[\\dLMNP-V]{2}(?:[A-EHLMPR-T](?:[04LQ][1-9MNP-V]|[15MR][\\dLMNP-V]|[26NS][0-8LMNP-U])|[DHPS][37PT][0L]|[ACELMRT][37PT][01LM]|[AC-EHLMPR-T][26NS][9V])|(?:[02468LNQSU][048LQU]|[13579MPRTV][26NS])B[26NS][9V])(?:[A-MZ][1-9MNP-V][\\dLMNP-V]{2}|[A-M][0L](?:[1-9MNP-V][\\dLMNP-V]|[0L][1-9MNP-V]))[A-Z]$");
  private static final Pattern FISCAL_CODE_LEGAL_ENTITY_PATTERN = Pattern.compile("^\\d{11}$");
  private static final Pattern ITALIAN_IBAN_PATTERN = Pattern.compile("^IT\\d{2}[A-Z]\\d{5}\\d{5}[A-Z0-9]{12}$");

  private ValidationUtils() {
    // Utility class, no instantiation
  }

  public static boolean isValidLegacyPaymentMetadata(final String legacyPaymentMetadata) {
    return legacyPaymentMetadata!=null && LEGACY_PAYMENT_METADATA_PATTERN.matcher(legacyPaymentMetadata).matches();
  }

  public static boolean isValidLegacyPaymentMetadataSecondary(final String legacyPaymentMetadata) {
    return legacyPaymentMetadata!=null && LEGACY_PAYMENT_METADATA_SECONDARY_PATTERN.matcher(legacyPaymentMetadata).matches();
  }

  public static String getTransferCategoryFromLegacyPaymentMetadataSecondary(final String legacyPaymentMetadata) {
    if(StringUtils.isBlank(legacyPaymentMetadata))
      return null;
    Matcher matcher = LEGACY_PAYMENT_METADATA_SECONDARY_PATTERN.matcher(legacyPaymentMetadata);
    return matcher.matches() ? matcher.group(1) : null;
  }

  public static boolean isValidEmail(final String email) {
    return email!=null && EMAIL_PATTERN.matcher(email).matches();
  }

  public static boolean verifyBalanceAmount(Bilancio balance, BigDecimal amount) {
    if(balance == null){
      return true;
    }
    BigDecimal balanceAmount = balance.getCapitolos().stream()
      .flatMap(capitolo -> capitolo.getAccertamentos().stream())
      .map(CtAccertamento::getImporto)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    return balanceAmount.compareTo(amount) == 0;
  }

  public static boolean verifyValidAnonymousDebtor(DebtPositionTypeOrg debtPositionTypeOrg, PersonDTO personDTO) {
    return debtPositionTypeOrg!=null && personDTO!=null && (
      StringUtils.equals(personDTO.getFiscalCode(), Constants.ANONYMOUS_FISCAL_CODE) &&
      EntityTypeEnum.F.equals(personDTO.getEntityType()) &&
      Boolean.TRUE.equals(debtPositionTypeOrg.getFlagAnonymousFiscalCode()) ||
      !StringUtils.equals(personDTO.getFiscalCode(), Constants.ANONYMOUS_FISCAL_CODE));
  }

  public static boolean isValidISOCountry(final String s) {
    return s!=null && ISO_COUNTRIES.contains(s);
  }

  public static boolean isValidProvince(final String province) {
    return province!=null && PROVINCE_PATTERN.matcher(province).matches();
  }

  public static boolean isValidPostalCode(final String postalCode, final String countryCode) {
    return postalCode!=null && (Locale.ITALY.getCountry().equals(countryCode)?POSTAL_CODE_IT_PATTERN:POSTAL_CODE_PATTERN).matcher(postalCode).matches();
  }

  public static boolean isValidCivic(final String civic) {
    return civic!=null && CIVIC_PATTERN.matcher(civic).matches();
  }

  public static boolean isValidFiscalCodeNaturalPerson(final String fiscalCode) {
    return fiscalCode!=null && FISCAL_CODE_NATURAL_PERSON_PATTERN.matcher(fiscalCode).matches();
  }

  public static boolean isValidFiscalCodeLegalEntity(final String fiscalCode) {
    return fiscalCode!=null && FISCAL_CODE_LEGAL_ENTITY_PATTERN.matcher(fiscalCode).matches();
  }

  public static boolean isValidIban(final String iban) {
    return iban != null && ITALIAN_IBAN_PATTERN.matcher(iban).matches();
  }

  public static boolean isValidUri(final String uri) {
    if (StringUtils.isBlank(uri)) {
      return false;
    }
    try {
      new URI(uri);
      return true;
    } catch (URISyntaxException e) {
      return false;
    }
  }

}
