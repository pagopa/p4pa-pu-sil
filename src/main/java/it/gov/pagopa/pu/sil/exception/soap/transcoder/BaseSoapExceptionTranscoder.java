package it.gov.pagopa.pu.sil.exception.soap.transcoder;

import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.common.BaseBusinessException;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.exception.soap.SoapFaultTranscoded;
import org.slf4j.Logger;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.Map;

public abstract class BaseSoapExceptionTranscoder {

  private final SilFaults unauthorizedFault;
  private final SilFaults systemErrorFault;
  private final Map<String, SilFaults> errorCode2SilFault;

  protected BaseSoapExceptionTranscoder(
    SilFaults unauthorizedFault,
    SilFaults systemErrorFault,
    Map<String, SilFaults> errorCode2SilFault
  ) {
    this.unauthorizedFault = unauthorizedFault;
    this.systemErrorFault = systemErrorFault;
    this.errorCode2SilFault = errorCode2SilFault;
  }

  protected abstract Logger getLogger();

  public SoapFaultTranscoded transcodeException(Exception exception) {
    SoapFaultTranscoded out;
    if (exception instanceof AuthorizationDeniedException) {
      out = new SoapFaultTranscoded(
        unauthorizedFault,
        "Utente non autorizzato"
      );
    } else if (exception instanceof SilFaultException sfe) {
      out = new SoapFaultTranscoded(
        sfe.getFault(),
        sfe.getDescription()
      );
    } else if ((out = transcodeSoapServiceExceptions(exception)) == null && exception instanceof BaseBusinessException bbe) {
      SilFaults fault = errorCode2SilFault.get(bbe.getCode());
      if (fault != null) {
        out = new SoapFaultTranscoded(fault, fault.description());
      }
    }

    if(out == null) {
      if(exception instanceof BaseBusinessException bbe) {
        getLogger().warn("Error code not mapped: {}", bbe.getCode());
      } else {
        getLogger().error("System error occurred: [{}] {}", exception.getClass().getSimpleName(), exception.getMessage(), exception);
      }

      out = new SoapFaultTranscoded(
        systemErrorFault,
        "Errore di sistema"
      );
    }

    return out;
  }

  /** to transcode Exception specific of Soap service */
  protected abstract SoapFaultTranscoded transcodeSoapServiceExceptions(Exception exception);
}
