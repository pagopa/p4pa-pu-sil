package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.ReceiptDTO;

public interface ReceiptService {

  /**
   * Retrieves a receipt by its ID.
   *
   * @param receiptId the ID of the receipt
   * @param accessToken the access token for authentication
   * @return the ReceiptDTO associated with the given receipt ID
   */
  ReceiptDTO getReceiptById(Long receiptId, String accessToken);
}
