package it.gov.pagopa.pu.sil.exception;

import lombok.Getter;

@Getter
public class ExportFileClientException extends BaseBusinessException {

  public ExportFileClientException(BaseBusinessException e) {
    super(e.getCode(), e.getMessage(), e.getCause());
  }
}
