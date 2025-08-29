package it.gov.pagopa.pu.sil.connector.debtpositions.client;

import it.gov.pagopa.pu.debtpositions.dto.generated.ReceiptDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.config.DebtPositionsApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
@Slf4j
public class ReceiptClient {

  private final DebtPositionsApisHolder debtPositionsApisHolder;

  public ReceiptClient(DebtPositionsApisHolder debtPositionsApisHolder) {
    this.debtPositionsApisHolder = debtPositionsApisHolder;
  }

  public ReceiptDTO getReceiptById(Long receiptId, String accessToken) {
    try {
      return debtPositionsApisHolder
        .getReceiptApi(accessToken)
        .getReceipt(receiptId);
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Cannot find ReceiptDTO having receiptId[{}]", receiptId, e);
      return null;
    }
  }

}
