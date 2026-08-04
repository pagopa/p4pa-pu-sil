package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.exception.common.BaseBusinessException;
import lombok.Getter;

@Getter
public class ExportFileServiceException extends BaseBusinessException {

  public ExportFileServiceException(String code, String message) {
    super(code, message);
  }
}
