package it.gov.pagopa.pu.sil.connector.fileshare;

import it.gov.pagopa.pu.sil.connector.fileshare.client.FileShareClient;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class FileShareServiceImpl implements FileShareService {

  private final FileShareClient client;

  public FileShareServiceImpl(FileShareClient client) {
    this.client = client;
  }

  @Override
  public Resource downloadReceipt(Long organizationId, Long receiptId, String accessToken) {
    return client.downloadReceipt(organizationId, receiptId, accessToken);
  }
}
