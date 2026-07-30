package it.gov.pagopa.pu.sil.service.receipt;

import it.gov.pagopa.pu.sil.connector.fileshare.FileShareService;
import it.gov.pagopa.pu.sil.exception.IllegalStateBusinessException;
import it.gov.pagopa.pu.sil.exception.ResourceNotFoundException;
import it.gov.pagopa.pu.sil.util.ErrorCodeConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

  @Mock
  private FileShareService fileShareServiceMock;

  @InjectMocks
  private ReceiptService receiptService;

  @Test
  void getReceiptByIdReturnsContentWhenReceiptExists() {
    Long receiptId = 1L;
    Long organizationId = 2L;
    String accessToken = "validToken";
    byte[] expectedContent = new byte[]{1, 2, 3};
    ByteArrayResource expectedResource = new ByteArrayResource(expectedContent);
    when(fileShareServiceMock.downloadReceipt(organizationId, receiptId, accessToken)).thenReturn(expectedResource);

    byte[] result = receiptService.getReceiptById(receiptId, organizationId, accessToken);

    assertArrayEquals(expectedContent, result);
  }

  @Test
  void getReceiptByIdThrowsApplicationExceptionWhenReceiptNotFound() {
    Long receiptId = 1L;
    Long organizationId = 2L;
    String accessToken = "validToken";
    when(fileShareServiceMock.downloadReceipt(organizationId, receiptId, accessToken)).thenReturn(null);

    ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
      receiptService.getReceiptById(receiptId, organizationId, accessToken)
    );

    assertEquals(ErrorCodeConstants.ERROR_CODE_RECEIPT_NOT_FOUND, exception.getCode());
    assertEquals("Receipt with id %s not found ".formatted(receiptId), exception.getMessage());
  }

  @Test
  void getReceiptByIdThrowsApplicationExceptionWhenIOExceptionOccurs() throws IOException {
    Long receiptId = 1L;
    Long organizationId = 2L;
    String accessToken = "validToken";
    Resource resourceMock = mock(ByteArrayResource.class);

    when(fileShareServiceMock.downloadReceipt(organizationId, receiptId, accessToken)).thenReturn(resourceMock);
    when(resourceMock.getContentAsByteArray()).thenThrow(new IOException("IO error"));

    IllegalStateBusinessException exception = assertThrows(IllegalStateBusinessException.class, () ->
      receiptService.getReceiptById(receiptId, organizationId, accessToken)
    );

    assertEquals(ErrorCodeConstants.ERROR_CODE_RECEIPT_ERROR, exception.getCode());
    assertEquals("java.io.IOException: IO error", exception.getCause().toString());
  }
}
