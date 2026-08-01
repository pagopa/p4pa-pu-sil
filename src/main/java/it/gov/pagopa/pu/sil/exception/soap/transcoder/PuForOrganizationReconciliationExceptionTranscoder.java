package it.gov.pagopa.pu.sil.exception.soap.transcoder;

import it.gov.pagopa.pu.processexecutions.dto.generated.ProcessExecutionsErrorDTO;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ExportFileServiceException;
import it.gov.pagopa.pu.sil.exception.IngestionFlowFileTypeValidationException;
import it.gov.pagopa.pu.sil.exception.soap.SoapFaultTranscoded;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class PuForOrganizationReconciliationExceptionTranscoder extends BaseSoapExceptionTranscoder {

  protected static final Map<String, SilFaults> errorCode2SilFault = Map.of(
    ProcessExecutionsErrorDTO.CategoryEnum.PROCESS_EXECUTIONS_INVALID_FILE_VERSION.getValue(), SilFaults.PIVOT_VERSIONE_TRACCIATO_NON_VALIDA,
    ProcessExecutionsErrorDTO.CategoryEnum.PROCESS_EXECUTIONS_INVALID_TIME_RANGE.getValue(), SilFaults.PIVOT_INTERVALLO_DATE_NON_VALIDO
  );

  public PuForOrganizationReconciliationExceptionTranscoder() {
    super(SilFaults.PIVOT_ENTE_NON_VALIDO, SilFaults.PIVOT_SYSTEM_ERROR, errorCode2SilFault);
  }

  @Override
  protected Logger getLogger() {
    return log;
  }

  @Override
  protected SoapFaultTranscoded transcodeSoapServiceExceptions(Exception exception) {
    if (exception instanceof IngestionFlowFileTypeValidationException iftve) {
      return new SoapFaultTranscoded(SilFaults.PIVOT_TIPO_FLUSSO_NON_VALIDO, "Tipo di flusso non valido: %s".formatted(iftve.getRejectedValue()));
    } else if (exception instanceof ExportFileServiceException efse) {
      SilFaults fault = null;
      if(ErrorCodeConstants.ERROR_CODE_INVALID_DEBT_POSITION_TYPE_ORG_CODE.equals(efse.getCode())) {
        fault = SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_VALIDO;
      } else if (ErrorCodeConstants.ERROR_CODE_INVALID_DEBT_POSITION_TYPE_ORG_STATUS.equals(efse.getCode())) {
        fault = SilFaults.PIVOT_IDENTIFICATIVO_TIPO_DOVUTO_NON_ABILITATO;
      }

      if(fault != null) {
        return new SoapFaultTranscoded(fault, fault.description() + ": " + efse.getMessage());
      }
    }

    return null;
  }
}
