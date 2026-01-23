package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiSingoloVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtSoggettoPagatore;
import it.veneto.regione.schemas._2012.pagamenti.ente.Dovuti;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.co.jemos.podam.api.PodamFactory;

import static org.junit.jupiter.api.Assertions.*;

class DovutiMapperTest {
  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  private DovutiMapper dovutiMapper = new DovutiMapper();

  @ParameterizedTest
  @CsvSource(value={
    "remittanceInformation, null, remittanceInformation",
    "remittanceInformation, originalRemittanceInformation, originalRemittanceInformation"
  }, nullValues={"null"})
  void testMap(String remittanceInformation, String originalRemittanceInformation, String expectedRemittanceInformation) {
    // Given
    PersonDTO debtor = new PersonDTO();
    debtor.setFiscalCode("RSSMRA80A01H501U");
    debtor.entityType(PersonEntityType.F);
    debtor.setFullName("Mario Rossi");
    debtor.setAddress("Via Roma 1");
    debtor.setCivic("1");
    debtor.setPostalCode("00100");
    debtor.setLocation("Roma");
    debtor.setProvince("RM");
    debtor.setNation("IT");
    debtor.setEmail("mario.rossi@example.com");
    debtor.setEntityType(PersonEntityType.F);
    InstallmentDTO installment = podamFactory.manufacturePojo(InstallmentDTO.class);
    installment.setDebtor(debtor);
    installment.setIuv("IUV123");
    installment.setIud("IUD123");
    installment.setAmountCents(15000L); // 150.00 EUR
    installment.setRemittanceInformation(remittanceInformation);
    installment.setOriginalRemittanceInformation(originalRemittanceInformation);
    installment.setLegacyPaymentMetadata("Legacy metadata info");

    DebtPositionTypeOrg debtPositionTypeOrg = podamFactory.manufacturePojo(DebtPositionTypeOrg.class);
    debtPositionTypeOrg.setCode("DPTO123");

    // When
    Dovuti result = dovutiMapper.map(installment, debtPositionTypeOrg);
    // Then
    assertNotNull(result);
    CtSoggettoPagatore soggettoPagatoreResult = result.getSoggettoPagatore();
    assertNotNull(soggettoPagatoreResult);
    assertEquals(debtor.getFiscalCode(), soggettoPagatoreResult.getIdentificativoUnivocoPagatore().getCodiceIdentificativoUnivoco());
    assertEquals(debtor.getEntityType().getValue(), soggettoPagatoreResult.getIdentificativoUnivocoPagatore().getTipoIdentificativoUnivoco().value());
    assertEquals(debtor.getFullName(), soggettoPagatoreResult.getAnagraficaPagatore());
    assertEquals(debtor.getAddress(), soggettoPagatoreResult.getIndirizzoPagatore());
    assertEquals(debtor.getCivic(), soggettoPagatoreResult.getCivicoPagatore());
    assertEquals(debtor.getPostalCode(), soggettoPagatoreResult.getCapPagatore());
    assertEquals(debtor.getLocation(), soggettoPagatoreResult.getLocalitaPagatore());
    assertEquals(debtor.getProvince(), soggettoPagatoreResult.getProvinciaPagatore());
    assertEquals(debtor.getNation(), soggettoPagatoreResult.getNazionePagatore());
    assertEquals(debtor.getEmail(), soggettoPagatoreResult.getEMailPagatore());
    CtDatiVersamentoDovuti datiVersamentoDovutiResult = result.getDatiVersamento();
    assertEquals(installment.getIuv(), datiVersamentoDovutiResult.getIdentificativoUnivocoVersamento());
    CtDatiSingoloVersamentoDovuti datiSingoloVersamentoResult = datiVersamentoDovutiResult.getDatiSingoloVersamentos().getFirst();
    assertEquals(installment.getIud(), datiSingoloVersamentoResult.getIdentificativoUnivocoDovuto());
    assertEquals(ConversionUtils.centsAmountToBigDecimalEuroAmount(installment.getAmountCents()), datiSingoloVersamentoResult.getImportoSingoloVersamento());
    assertEquals(debtPositionTypeOrg.getCode(), datiSingoloVersamentoResult.getIdentificativoTipoDovuto());
    assertEquals(expectedRemittanceInformation, datiSingoloVersamentoResult.getCausaleVersamento());
    assertEquals(installment.getLegacyPaymentMetadata(), datiSingoloVersamentoResult.getDatiSpecificiRiscossione());
  }
}
