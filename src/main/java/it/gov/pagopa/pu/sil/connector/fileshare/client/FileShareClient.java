package it.gov.pagopa.pu.sil.connector.fileshare.client;

import it.gov.pagopa.pu.sil.connector.fileshare.config.FileShareApisHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FileShareClient {

  private final FileShareApisHolder apisHolder;

  public FileShareClient(FileShareApisHolder apisHolder){
    this.apisHolder = apisHolder;
  }

  public Resource downloadReceipt(Long organizationId, Long receiptId, String accessToken) {
    return apisHolder.getReceiptApi(accessToken)
      .downloadRt(organizationId, receiptId);
  }


}
