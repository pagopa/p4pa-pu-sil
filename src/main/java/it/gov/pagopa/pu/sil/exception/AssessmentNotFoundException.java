package it.gov.pagopa.pu.sil.exception;

import it.gov.pagopa.pu.sil.exception.common.BaseBusinessException;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;

public class AssessmentNotFoundException extends BaseBusinessException {

  public AssessmentNotFoundException(String message) {
    super(ErrorCodeConstants.ERROR_CODE_ASSESSMENT_NOT_FOUND, message);
  }

}
