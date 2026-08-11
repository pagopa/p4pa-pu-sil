package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.ReceiptDTO;
import it.gov.pagopa.pu.sil.connector.debtpositions.ReceiptService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.gov.pagopa.sil.paymentnotification.dto.generated.PaymentDataDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.stream.Stream;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationMapperTest {

  @Mock
  private ReceiptService receiptServiceMock;

  @InjectMocks
  private PaymentNotificationMapper paymentNotificationMapper;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @ParameterizedTest
  @MethodSource("provideRemittanceInformation")
  void testMapPaymentData(String remittanceInformation, String originalRemittanceInformation) {
    InstallmentDTO installmentDTO = podamFactory.manufacturePojo(InstallmentDTO.class);
    installmentDTO.setRemittanceInformation(remittanceInformation);
    installmentDTO.setOriginalRemittanceInformation(originalRemittanceInformation);
    ReceiptDTO receiptDTO = podamFactory.manufacturePojo(ReceiptDTO.class);
    when(receiptServiceMock.getReceiptById(installmentDTO.getReceiptId(), "accessToken"))
        .thenReturn(receiptDTO);

    PaymentDataDTO paymentDataDTO = paymentNotificationMapper.mapPaymentData(installmentDTO, "orgFiscalCode", "debtPositionTypeOrgCode", "accessToken");

    Assertions.assertNotNull(paymentDataDTO);
    String expectedRemittanceInformation = originalRemittanceInformation != null ? originalRemittanceInformation : remittanceInformation;
    Assertions.assertEquals(expectedRemittanceInformation, paymentDataDTO.getRemittanceInformation());
    TestUtils.checkAllNotNullFields(paymentDataDTO);
  }

  private static Stream<Arguments> provideRemittanceInformation() {
    return Stream.of(
      Arguments.of("remittanceInformation", null),
      Arguments.of("remittanceInformation", "originalRemittanceInformation")
    );
  }
}
