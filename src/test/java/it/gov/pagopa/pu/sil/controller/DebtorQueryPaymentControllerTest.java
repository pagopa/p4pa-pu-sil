package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType;
import it.gov.pagopa.pu.sil.dto.generated.PaymentHistoryResponseDTO;
import it.gov.pagopa.pu.sil.dto.generated.UnpaidDebtPositionsResponseDTO;
import it.gov.pagopa.pu.sil.security.SecurityUtilsTest;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.service.querypayments.DebtorQueryPaymentService;
import it.gov.pagopa.pu.sil.service.querypayments.DebtorQueryUnpaidDebtPositionService;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.co.jemos.podam.api.PodamFactory;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebtorQueryPaymentControllerTest {
  @Mock
  private DebtorQueryPaymentService debtorQueryPaymentServiceMock;
  @Mock
  private DebtorQueryUnpaidDebtPositionService debtorQueryUnpaidDebtPositionServiceMock;

  @InjectMocks
  private DebtorQueryPaymentController controller;

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();
  private final String accessToken = "fakeAccessToken";
  private final String orgFiscalCode = "ORG123456789";
  private final String orgIpaCode = "userIpaCode";
  private UserInfo userInfo;

  @BeforeEach
  void setUp() {
    userInfo = AuthorizationServiceTest.buildAdminUser(1L, orgFiscalCode, orgIpaCode);
    userInfo.setMappedExternalUserId("fakeExternalUser");
    SecurityUtilsTest.configureSecurityContext(accessToken, userInfo);
  }

  @AfterEach
  void clear() { SecurityUtilsTest.clearSecurityContext(); }

  @Test
  void whenGetPaymentHistoryThenOk() {
    // Given
    PersonEntityType debtorEntityType = PersonEntityType.F;
    String debtorFiscalCode = "RSSMRA80A01H501U";
    OffsetDateTime dateFrom = OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS);
    OffsetDateTime dateTo =  OffsetDateTime.now().plusDays(1).truncatedTo(ChronoUnit.DAYS);

    PaymentHistoryResponseDTO expectedResponse = podamFactory.manufacturePojo(PaymentHistoryResponseDTO.class);

    // When
    when(debtorQueryPaymentServiceMock.processRequest(
      any(),
      eq(userInfo),
      eq(accessToken)
    )).thenReturn(expectedResponse);

    ResponseEntity<PaymentHistoryResponseDTO> responseEntity = controller
      .getPaymentHistory(debtorFiscalCode, debtorEntityType, dateFrom, dateTo, orgIpaCode);

    // Then
    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(expectedResponse, responseEntity.getBody());
  }

  @Test
  void whenGetUnpaidDebtPositionsThenOk() {
    // Given
    PersonEntityType debtorEntityType = PersonEntityType.F;
    String debtorFiscalCode = "RSSMRA80A01H501U";

    UnpaidDebtPositionsResponseDTO expectedResponse = podamFactory.manufacturePojo(UnpaidDebtPositionsResponseDTO.class);

    // When
    when(debtorQueryUnpaidDebtPositionServiceMock.processRequest(
      any(),
      eq(userInfo),
      eq(accessToken)
    )).thenReturn(expectedResponse);

    ResponseEntity<UnpaidDebtPositionsResponseDTO> responseEntity = controller
      .getUnpaidDebtPositions(debtorFiscalCode, debtorEntityType, orgIpaCode);

    // Then
    assertNotNull(responseEntity);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(expectedResponse, responseEntity.getBody());
  }
}
