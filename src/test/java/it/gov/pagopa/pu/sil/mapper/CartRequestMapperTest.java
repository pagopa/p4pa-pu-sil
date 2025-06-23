package it.gov.pagopa.pu.sil.mapper;

import it.gov.pagopa.nodo.checkout.dto.generated.CartRequest;
import it.gov.pagopa.pu.debtpositions.dto.generated.*;
import it.gov.pagopa.pu.sil.enums.SilFaults;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.apache.commons.lang3.tuple.Triple;
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
      .entityType(EntityTypeEnum.G)
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
    Triple<CartRequest, SilFaults, String> result = cartRequestMapper.mapDebtPositionsToCartRequest(
      List.of(fullDebtPositionDTO), cartId, callbackUrl);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getLeft());
    assertEquals(cartId, result.getLeft().getIdCart());
    assertEquals(fullDebtPositionDTO.getPaymentOptions().getFirst().getInstallments().getFirst().getDebtor().getEmail(), result.getLeft().getEmailNotice());
    assertEquals(num, result.getLeft().getPaymentNotices().size());
    TestUtils.checkNotNullFields(result.getLeft());
  }

  @Test
  void testMapDebtPositionsToCartRequest_NullCallbackUrl() {
    // Arrange
    String cartId = "cart123";

    // Act
    Triple<CartRequest, SilFaults, String> result = cartRequestMapper.mapDebtPositionsToCartRequest(
      List.of(debtPosition), cartId, null);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getLeft());
    assertEquals("http://TODO.com", result.getLeft().getReturnUrls().getReturnOkUrl().toString());
    TestUtils.checkNotNullFields(result.getLeft());
  }

  @Test
  void testMapDebtPositionsToCartRequest_InvalidCallbackUrl() {
    // Arrange
    String cartId = "cart123";
    String invalidCallbackUrl = "http://";

    // Act & Assert
    assertThrows(ApplicationException.class, () -> cartRequestMapper.mapDebtPositionsToCartRequest(
      List.of(debtPosition), cartId, invalidCallbackUrl));
  }

  @Test
  void testMapDebtPositionsToCartRequest_AllCCPTrue() {
    // Arrange
    String cartId = "cart123";
    String callbackUrl = "http://valid-url.com";

    // Act
    Triple<CartRequest, SilFaults, String> result = cartRequestMapper.mapDebtPositionsToCartRequest(
      List.of(debtPosition), cartId, callbackUrl);

    // Assert
    assertEquals(Boolean.TRUE, result.getLeft().getAllCCP());
    TestUtils.checkNotNullFields(result.getLeft());
  }

  @Test
  void testMapDebtPositionsToCartRequest_AllCCPFalse() {
    // Arrange
    String cartId = "cart123";
    String callbackUrl = "http://valid-url.com";
    debtPosition.getPaymentOptions().getFirst().getInstallments().getFirst().getTransfers().getFirst().setPostalIban(null);

    // Act
    Triple<CartRequest, SilFaults, String> result = cartRequestMapper.mapDebtPositionsToCartRequest(
      List.of(debtPosition), cartId, callbackUrl);

    // Assert
    assertNotEquals(Boolean.TRUE, result.getLeft().getAllCCP());
    TestUtils.checkNotNullFields(result.getLeft());
  }

}
