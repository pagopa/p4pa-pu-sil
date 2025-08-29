package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.paymentnotification.dto.generated.PaymentDataDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.ReceiptDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.ReceiptService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

@ExtendWith(MockitoExtension.class)
class PyamentNotificationMapperTest {

  @Mock
  private ReceiptService receiptServiceMock;

  @InjectMocks
  private PaymentNotificationMapper paymentNotificationMapper;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @Test
  void testMapPaymentData(){
    InstallmentDTO installmentDTO = podamFactory.manufacturePojo(InstallmentDTO.class);
    ReceiptDTO receiptDTO = podamFactory.manufacturePojo(ReceiptDTO.class);
    Mockito.when(receiptServiceMock.getReceiptById(installmentDTO.getReceiptId(), "accessToken"))
        .thenReturn(receiptDTO);

    PaymentDataDTO paymentDataDTO = paymentNotificationMapper.mapPaymentData(installmentDTO, "orgFiscalCode", "debtPositionTypeOrgCode", "accessToken");

    Assertions.assertNotNull(paymentDataDTO);
    TestUtils.checkAllNotNullFields(paymentDataDTO);
  }

}
