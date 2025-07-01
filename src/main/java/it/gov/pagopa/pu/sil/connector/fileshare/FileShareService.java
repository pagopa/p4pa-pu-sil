package it.gov.pagopa.pu.sil.connector.fileshare;

import org.springframework.core.io.Resource;

public interface FileShareService {
  Resource downloadReceipt(Long organizationId, Long receiptId, String accessToken);
}
