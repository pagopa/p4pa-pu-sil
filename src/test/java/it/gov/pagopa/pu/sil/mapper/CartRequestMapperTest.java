package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CartRequestMapperTest {

  @InjectMocks
  private CartRequestMapper cartRequestMapper;

  private DebtPositionDTO debtPosition = null;
  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    cartRequestMapper = new CartRequestMapper(
      "http://ok.TEST.com",
      "http://ko.TEST.com",
      "http://cancel.TEST.com"
    );

    TransferDTO transfer = new TransferDTO();
    transfer.setOrgFiscalCode("12345678901");
    transfer.setOrgName("OrgName");
    transfer.setPostalIban("IT60X0542811101000000123456");

    InstallmentDTO installment = new InstallmentDTO();
    installment.setNav("12345");
    installment.setAmountCents(1000L);
    installment.setRemittanceInformation("Payment description");
    installment.setTransfers(List.of(transfer));
    installment.setDebtor(PersonDTO.builder()
      .entityType(PersonEntityType.G)
      .fiscalCode("12345678901")
      .fullName("John Doe")
      .email("validEmail@example.com")
      .build());

    PaymentOptionDTO paymentOption = new PaymentOptionDTO();
    paymentOption.setInstallments(List.of(installment));

    debtPosition = new DebtPositionDTO();
    debtPosition.setPaymentOptions(List.of(paymentOption));
  }

  @Test
  void testMapDebtPositionsToCartRequest_ValidInput() {
    // Arrange
    String cartId = "cart123";
    String callbackUrl = "http://valid-url.com";
    DebtPositionDTO fullDebtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    int num = fullDebtPositionDTO.getPaymentOptions().stream().mapToInt(po -> po.getInstallments().size()).sum();

    // Act
    CartRequest result = cartRequestMapper.mapDebtPositionsToCartRequest(
      List.of(fullDebtPositionDTO), cartId, callbackUrl);

    // Assert
    assertNotNull(result);
    assertEquals(cartId, result.getIdCart());
    assertEquals(fullDebtPositionDTO.getPaymentOptions().getFirst().getInstallments().getFirst().getDebtor().getEmail(), result.getEmailNotice());
    assertEquals(num, result.getPaymentNotices().size());
    assertEquals(callbackUrl, result.getReturnUrls().getReturnOkUrl().toString());
    assertEquals(callbackUrl, result.getReturnUrls().getReturnErrorUrl().toString());
    assertEquals(callbackUrl, result.getReturnUrls().getReturnCancelUrl().toString());
    TestUtils.checkNotNullFields(result);
  }

  @Test
  void testMapDebtPositionsToCartRequest_NullCallbackUrl() {
    // Arrange
    String cartId = "cart123";

    // Act
    CartRequest result = cartRequestMapper.mapDebtPositionsToCartRequest(
      List.of(debtPosition), cartId, null);

    // Assert
    assertNotNull(result);
    assertEquals("http://ok.TEST.com", result.getReturnUrls().getReturnOkUrl().toString());
    assertEquals("http://ko.TEST.com", result.getReturnUrls().getReturnErrorUrl().toString());
    assertEquals("http://cancel.TEST.com", result.getReturnUrls().getReturnCancelUrl().toString());
    TestUtils.checkNotNullFields(result);
  }

  @Test
  void testMapDebtPositionsToCartRequest_InvalidCallbackUrl() {
    // Arrange
    String cartId = "cart123";
    String invalidCallbackUrl = "http://";
    List<DebtPositionDTO> debtPositions = List.of(debtPosition);

    // Act & Assert
    assertThrows(ApplicationException.class, () -> cartRequestMapper.mapDebtPositionsToCartRequest(
      debtPositions, cartId, invalidCallbackUrl));
  }

  @Test
  void testMapDebtPositionsToCartRequest_AllCCPTrue() {
    // Arrange
    String cartId = "cart123";
    String callbackUrl = "http://valid-url.com";

    // Act
    CartRequest result = cartRequestMapper.mapDebtPositionsToCartRequest(
      List.of(debtPosition), cartId, callbackUrl);

    // Assert
    assertEquals(Boolean.TRUE, result.getAllCCP());
    TestUtils.checkNotNullFields(result);
  }

  @Test
  void testMapDebtPositionsToCartRequest_AllCCPFalse() {
    // Arrange
    String cartId = "cart123";
    String callbackUrl = "http://valid-url.com";
    debtPosition.getPaymentOptions().getFirst().getInstallments().getFirst().getTransfers().getFirst().setPostalIban(null);

    // Act
    CartRequest result = cartRequestMapper.mapDebtPositionsToCartRequest(
      List.of(debtPosition), cartId, callbackUrl);

    // Assert
    assertNotEquals(Boolean.TRUE, result.getAllCCP());
    TestUtils.checkNotNullFields(result);
  }

}
