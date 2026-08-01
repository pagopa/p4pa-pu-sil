package it.gov.pagopa.pu.sil.exception.soap.transcoder;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.exception.soap.SoapFaultTranscoded;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class PuForOrganizationPaymentExceptionTranscoder extends BaseSoapExceptionTranscoder {

  protected static final Map<String, SilFaults> errorCode2SilFault = Map.ofEntries(
    Map.entry("INVALID_FILE_VERSION", SilFaults.PAA_VERSIONE_TRACCIATO_NON_VALIDA),

    Map.entry("INVALID_DATE_FILTER_INTERVAL", SilFaults.PAA_INTERVALLO_DATE_NON_VALIDO),
    Map.entry("INVALID_DATE_FILTER_COMBINATION", SilFaults.PAA_INTERVALLO_DATE_NON_VALIDO),

    Map.entry("INVALID_ORGANIZATION", SilFaults.PAA_ENTE_NON_VALIDO),
    Map.entry("INVALID_ORGANIZATION_STATUS", SilFaults.PAA_ENTE_NON_VALIDO),

    Map.entry("INVALID_IUV", SilFaults.PAA_IUV_NON_VALIDO),
    Map.entry("MISSING_IUV", SilFaults.PAA_IUV_NON_VALIDO),

    Map.entry("MISSING_DEBT_POSITION_TYPE_ORG", SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO),

    Map.entry("DEBT_POSITION_TYPE_ORG_UNAUTHORIZED", SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO),

    Map.entry("INVALID_AMOUNT", SilFaults.PAA_IMPORTO_SINGOLO_VERSAMENTO_NON_VALIDO),
    Map.entry("INVALID_CENTS_AMOUNT", SilFaults.PAA_IMPORTO_SINGOLO_VERSAMENTO_NON_VALIDO),

    Map.entry("INVALID_TAXONOMY_CATEGORY", SilFaults.PAA_DATI_SPECIFICI_RISCOSSIONE_NON_VALIDO),
    Map.entry("MISSING_TAXONOMY_CATEGORY", SilFaults.PAA_DATI_SPECIFICI_RISCOSSIONE_NON_VALIDO),
    Map.entry("INVALID_LEGACY_PAYMENT_METADATA", SilFaults.PAA_DATI_SPECIFICI_RISCOSSIONE_NON_VALIDO),

    Map.entry("INVALID_BALANCE", SilFaults.PAA_IMPORTO_BILANCIO_NON_VALIDO),

    Map.entry("MISSING_REMITTANCE_INFORMATION", SilFaults.PAA_CAUSALE_NON_PRESENTE),

    Map.entry("INVALID_FULLNAME", SilFaults.PAA_ANAGRAFICA_NON_VALIDA),
    Map.entry("INVALID_EMAIL", SilFaults.PAA_ANAGRAFICA_NON_VALIDA),
    Map.entry("MISSING_DEBTOR", SilFaults.PAA_ANAGRAFICA_NON_VALIDA),

    Map.entry("INVALID_VAT_CODE", SilFaults.PAA_CODICE_FISCALE_NON_VALIDO),

    Map.entry("TOO_MANY_TRANSFERS_FOR_INSTALLMENT", SilFaults.PAA_LIMITE_MASSIMO_DOVUTI_MULTIBENEFICIARI),

    Map.entry("INVALID_IBAN", SilFaults.PAA_ENTE_SECONDARIO_NON_VALIDO),

    Map.entry("IMMUTABLE_FIELD", SilFaults.PAA_CAMPO_NON_MODIFICABILE),

    Map.entry("MISSING_DUE_DATE", SilFaults.PAA_DATA_ESECUZIONE_PAGAMENTO_NON_VALIDA),
    Map.entry("INVALID_DUE_DATE", SilFaults.PAA_DATA_ESECUZIONE_PAGAMENTO_NON_VALIDA),

    Map.entry("INSTALLMENT_ALREADY_EXISTS", SilFaults.PAA_DOVUTO_DUPLICATO),
    Map.entry("DEBT_POSITION_ALREADY_EXISTS", SilFaults.PAA_DOVUTO_DUPLICATO)
  );

  public PuForOrganizationPaymentExceptionTranscoder() {
    super(SilFaults.PAA_ENTE_NON_VALIDO, SilFaults.PAA_SYSTEM_ERROR, errorCode2SilFault);
  }

  @Override
  protected Logger getLogger() {
    return log;
  }

  @Override
  protected SoapFaultTranscoded transcodeSoapServiceExceptions(Exception exception) {
    if (exception instanceof ExportFileServiceException efse) {
      SilFaults fault = null;
      if(ErrorCodeConstants.ERROR_CODE_INVALID_DEBT_POSITION_TYPE_ORG_CODE.equals(efse.getCode())) {
        fault = SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO;
      } else if (ErrorCodeConstants.ERROR_CODE_INVALID_DEBT_POSITION_TYPE_ORG_STATUS.equals(efse.getCode())) {
        fault = SilFaults.PAA_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO;
      }

      if(fault != null) {
        return new SoapFaultTranscoded(fault, fault.description() + ": " + efse.getMessage());
      }
    }

    return null;
  }
}
