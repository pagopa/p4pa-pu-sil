package it.gov.pagopa.pu.sil.service.immediatepayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.util.Constants;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiMarcaBolloDigitale;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiSingoloVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtIdentificativoUnivocoPersonaFG;
import it.veneto.regione.schemas._2012.pagamenti.ente.StTipoIdentificativoUnivocoPersFG;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ValidationService {

  private final DebtPositionService debtPositionService;

  public ValidationService(DebtPositionService debtPositionService) {
    this.debtPositionService = debtPositionService;
  }

  public Pair<SilFaults, String> validateDebtPositionTypeOrg(DebtPositionTypeOrg debtPositionTypeOrg, String debtPositionTypeOrgCode) {
    if (debtPositionTypeOrg == null) {
      return Pair.of(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO, "Tipo dovuto non valido: " + debtPositionTypeOrgCode);
    } else if (Boolean.FALSE.equals(debtPositionTypeOrg.getFlagActive())) {
      return Pair.of(SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO, "Tipo dovuto non abilitato: " + debtPositionTypeOrgCode);
    }
    return null;
  }

  public Pair<SilFaults, String> validateStamp(CtDatiMarcaBolloDigitale stamp, String debtPositionTypeOrgCode) {
    if (Constants.STAMP_DEBT_POSITION_TYPE_ORG_CODE.equals(debtPositionTypeOrgCode)) {
      if (stamp == null ||
        StringUtils.isBlank(stamp.getTipoBollo()) ||
        StringUtils.isBlank(stamp.getHashDocumento()) ||
        StringUtils.isBlank(stamp.getProvinciaResidenza())) {
        return Pair.of(SilFaults.PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA, "Dati marca da bollo digitale non presenti");
      } else if (StringUtils.length(stamp.getHashDocumento()) > 72) {
        return Pair.of(SilFaults.PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA, "Hash documento marca da bollo digitale più lunga di 72 caratteri");
      }
    } else if (stamp == null) {
      return Pair.of(SilFaults.PAA_MARCA_BOLLO_DIGITALE_NON_VALIDA, "Dati marca da bollo digitale non previsti con tipo dovuto diverso da "
        + Constants.STAMP_DEBT_POSITION_TYPE_ORG_CODE);
    }
    return null;
  }

  public Pair<SilFaults, String> validateIud(Long orgId, String iud, String accessToken) {
    Long existingInstallmentWithIud = debtPositionService.countExistingInstallmentsByIudIuvNav(
      orgId, iud, null, null, accessToken);
    if (existingInstallmentWithIud > 0) {
      return Pair.of(SilFaults.PAA_IUD_DUPLICATO, "IUD duplicato: " + iud);
    }
    return null;
  }

  public Pair<SilFaults, String> validatePaymentData(CtDatiSingoloVersamentoDovuti versamento) {
    if (versamento.getImportoSingoloVersamento() == null || BigDecimal.ZERO.compareTo(versamento.getImportoSingoloVersamento()) >= 0) {
      return Pair.of(SilFaults.PAA_IMPORTO_SINGOLO_VERSAMENTO_NON_VALIDO, "Importo singolo versamento non valido: " + versamento.getImportoSingoloVersamento());
    }
    if (StringUtils.isBlank(versamento.getDatiSpecificiRiscossione()) || !ValidationUtils.isValidLegacyPaymentMetadata(versamento.getDatiSpecificiRiscossione())) {
      return Pair.of(SilFaults.PAA_DATI_SPECIFICI_RISCOSSIONE_NON_VALIDO, "Dati specifici riscossione non validi: " + versamento.getDatiSpecificiRiscossione());
    }
    if (!ValidationUtils.verifyBalanceAmount(versamento.getBilancio(), versamento.getImportoSingoloVersamento())) {
      return Pair.of(SilFaults.PAA_IMPORTO_BILANCIO_NON_VALIDO, "Importo bilancio non valido");
    }
    if (StringUtils.isBlank(versamento.getCausaleVersamento())) {
      return Pair.of(SilFaults.PAA_CAUSALE_NON_PRESENTE, "Causale versamento non presente o non valida");
    }
    return null;
  }

  public Pair<SilFaults, String> validateFiscalCodeDebtor(CtIdentificativoUnivocoPersonaFG personIdentifier) {
    if (personIdentifier == null) {
      return Pair.of(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, "Identificativo univoco persona non presente");
    } else if (StringUtils.isBlank(personIdentifier.getCodiceIdentificativoUnivoco()) ||
      personIdentifier.getTipoIdentificativoUnivoco() == null) {
      return Pair.of(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, "Identificativo univoco persona non valido");
    } else if (personIdentifier.getTipoIdentificativoUnivoco() == StTipoIdentificativoUnivocoPersFG.F &&
      !ValidationUtils.isValidFiscalCodeNaturalPerson(personIdentifier.getCodiceIdentificativoUnivoco())) {
      return Pair.of(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, "Codice fiscale persona fisica non valido: " + personIdentifier.getCodiceIdentificativoUnivoco());
    } else if (personIdentifier.getTipoIdentificativoUnivoco() == StTipoIdentificativoUnivocoPersFG.G &&
      !ValidationUtils.isValidFiscalCodeLegalEntity(personIdentifier.getCodiceIdentificativoUnivoco())) {
      return Pair.of(SilFaults.PAA_CODICE_FISCALE_NON_VALIDO, "Codice fiscale persona giuridica non valido: " + personIdentifier.getCodiceIdentificativoUnivoco());
    }
    return null;
  }

}
