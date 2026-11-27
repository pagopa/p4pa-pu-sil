package it.gov.pagopa.pu.sil.exception.soap;

import it.gov.pagopa.pu.sil.exception.soap.transcoder.PuForOrganizationReconciliationExceptionTranscoder;
import it.veneto.regione.pagamenti.pivot.ente.FaultBean;
import org.springframework.stereotype.Service;

@Service
public class PuForOrganizationReconciliationExceptionHandler extends BaseSoapExceptionHandler<FaultBean> {

  public PuForOrganizationReconciliationExceptionHandler(PuForOrganizationReconciliationExceptionTranscoder exceptionTranscoder) {
    super(exceptionTranscoder);
  }

  @Override
  protected FaultBean buildFault(String id, SoapFaultTranscoded faultTranscoded, int serial) {
    FaultBean out = new FaultBean();
    out.setId(id);
    out.setSerial(serial);
    out.setFaultCode(faultTranscoded.getSilFault().code());
    out.setFaultString(faultTranscoded.getSilFault().description());
    out.setDescription(faultTranscoded.getDescription());
    return out;
  }
}
