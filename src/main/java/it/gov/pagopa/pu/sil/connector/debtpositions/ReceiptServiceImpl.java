package it.gov.pagopa.pu.sil.connector.debtpositions;

import it.gov.pagopa.pu.debtpositions.dto.generated.ReceiptDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.client.ReceiptClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReceiptServiceImpl implements ReceiptService {

  private final ReceiptClient client;

  public ReceiptServiceImpl(ReceiptClient client) {
    this.client = client;
  }

  @Override
  public ReceiptDTO getReceiptById(Long receiptId, String accessToken) {
    return client.getReceiptById(receiptId, accessToken);
  }

}
