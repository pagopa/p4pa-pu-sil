package it.gov.pagopa.pu.sil.connector.pagopapayments;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import org.springframework.core.io.Resource;

/**
 * This interface provides a method for print payment notice on PagoPa service
 */
public interface PagopaPaymentsService {

  /**
   * Generates the PDF of a payment notice for a specific debt position.
   * @param iuv the IUV (Identificativo Unico di Versamento) of the payment notice
   * @param debtPosition the debt position details
   * @param accessToken the access token for authentication
   * @return a Resource containing the generated PDF notice
   */
  Resource generateNotice(String iuv, DebtPositionDTO debtPosition, String accessToken);
}
