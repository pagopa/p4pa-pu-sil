package it.gov.pagopa.pu.sil.connector.actualization;

import it.gov.pagopa.actualization.dto.generated.Payment;
import it.gov.pagopa.actualization.dto.generated.UpdatedPayment;
import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.organization.dto.generated.OrgSilServiceDTO;
import it.gov.pagopa.pu.sil.connector.actualization.client.ActualizationClient;
import org.springframework.stereotype.Component;


@Component
public class ActualizationServiceImpl implements ActualizationService {
  private final ActualizationClient actualizationClient;

  public ActualizationServiceImpl(ActualizationClient actualizationClient) {
    this.actualizationClient = actualizationClient;
  }

  @Override
  public UpdatedPayment actualization(OrgSilServiceDTO orgSilService, UserInfo loggedUser, String accessToken, Payment payment) {
    return actualizationClient.actualization(orgSilService, loggedUser, accessToken, payment);
  }

}
