package it.gov.pagopa.pu.sil.service.receipt;

import it.gov.pagopa.pu.sil.connector.fileshare.FileShareService;
import it.gov.pagopa.pu.sil.exception.IllegalStateBusinessException;
import it.gov.pagopa.pu.sil.exception.ResourceNotFoundException;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
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
          throw new IllegalStateBusinessException(ErrorCodeConstants.ERROR_CODE_RECEIPT_ERROR, "Something went wrong while retrieving receipt " + receiptId, ioe);
        }
      })
      .orElseThrow(() -> new ResourceNotFoundException(ErrorCodeConstants.ERROR_CODE_RECEIPT_NOT_FOUND, "Receipt with id %s not found ".formatted(receiptId)));
  }

}
