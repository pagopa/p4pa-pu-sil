package it.gov.pagopa.pu.sil.mapper.soap;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionTypeOrg;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.veneto.regione.schemas._2012.pagamenti.ente.*;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DovutiMapper {
  private static final String OBJECT_VERSION = "6.2.0";

  public Dovuti map(InstallmentDTO installment, DebtPositionTypeOrg debtPositionTypeOrg) {
    ObjectFactory of = new ObjectFactory();

    Dovuti dovuti = of.createDovuti();
    dovuti.setVersioneOggetto(OBJECT_VERSION);
    CtSoggettoPagatore soggettoPagatore = of.createCtSoggettoPagatore();
    CtIdentificativoUnivocoPersonaFG identificativoUnivocoPagatore = new CtIdentificativoUnivocoPersonaFG();
    identificativoUnivocoPagatore.setCodiceIdentificativoUnivoco(installment.getDebtor().getFiscalCode());
    identificativoUnivocoPagatore.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.fromValue(installment.getDebtor().getEntityType().getValue()));
    soggettoPagatore.setIdentificativoUnivocoPagatore(identificativoUnivocoPagatore);
    soggettoPagatore.setAnagraficaPagatore(installment.getDebtor().getFullName());
    soggettoPagatore.setIndirizzoPagatore(installment.getDebtor().getAddress());
    soggettoPagatore.setCivicoPagatore(installment.getDebtor().getCivic());
    soggettoPagatore.setCapPagatore(installment.getDebtor().getPostalCode());
    soggettoPagatore.setLocalitaPagatore(installment.getDebtor().getLocation());
    soggettoPagatore.setProvinciaPagatore(installment.getDebtor().getProvince());
    soggettoPagatore.setNazionePagatore(installment.getDebtor().getNation());
    soggettoPagatore.setEMailPagatore(installment.getDebtor().getEmail());

    CtDatiVersamentoDovuti datiVersamentoDovuti = of.createCtDatiVersamentoDovuti();
    datiVersamentoDovuti.setTipoVersamento("ALL");
    datiVersamentoDovuti.setIdentificativoUnivocoVersamento(installment.getIuv());
    CtDatiSingoloVersamentoDovuti datiSingoloVersamento = of.createCtDatiSingoloVersamentoDovuti();
    datiSingoloVersamento.setIdentificativoUnivocoDovuto(installment.getIud());
    datiSingoloVersamento.setImportoSingoloVersamento(ConversionUtils.centsAmountToBigDecimalEuroAmount(installment.getAmountCents()));

    if (debtPositionTypeOrg != null) {
      datiSingoloVersamento.setIdentificativoTipoDovuto(debtPositionTypeOrg.getCode());
    }

    datiSingoloVersamento.setCausaleVersamento(Optional.ofNullable(installment.getOriginalRemittanceInformation()).orElse(installment.getRemittanceInformation()));
    datiSingoloVersamento.setDatiSpecificiRiscossione(installment.getLegacyPaymentMetadata());

    datiVersamentoDovuti.getDatiSingoloVersamentos().add(datiSingoloVersamento);

    dovuti.setSoggettoPagatore(soggettoPagatore);
    dovuti.setDatiVersamento(datiVersamentoDovuti);

    return dovuti;
  }
}
