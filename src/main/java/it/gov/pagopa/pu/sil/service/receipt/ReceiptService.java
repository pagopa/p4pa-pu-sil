package it.gov.pagopa.pu.sil.service.receipt;

import it.gov.pagopa.pu.sil.connector.fileshare.FileShareService;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReceiptService {

  private final FileShareService fileShareService;

  public byte[] getReceiptById(Long receiptId, Long organizationId, String accessToken) {
    return Optional.ofNullable(fileShareService.downloadReceipt(organizationId, receiptId, accessToken))
      .map(resource -> {
        try {
          return resource.getContentAsByteArray();
        } catch (IOException ioe) {
          throw new ApplicationException(ioe);
        }
      })
      .orElseThrow(() -> new ApplicationException("receipt not found for id: " + receiptId));
  }

}
