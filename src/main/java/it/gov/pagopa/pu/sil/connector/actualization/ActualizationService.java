package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.dto.generated.Payment;
import it.gov.pagopa.actualization.dto.generated.UpdatedPayment;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;

public interface ActualizationService {
  UpdatedPayment actualization(OrgSilServiceDTO orgSilService, UserInfo loggedUser, String accessToken, Payment payment);
}
