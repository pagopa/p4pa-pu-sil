package it.gov.pagopa.pu.sil.service.inbound.payments.queryassessments.soap;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.classification.dto.generated.AssessmentsBalanceView;
import it.gov.pagopa.pu.sil.connector.classification.AssessmentService;
import it.gov.pagopa.pu.sil.exception.SilFaultException;
import it.gov.pagopa.pu.sil.mapper.soap.LegacyAssessmentsBalanceMapper;
import it.gov.pagopa.pu.sil.service.AuthorizationServiceTest;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.gov.pagopa.pu.sil.util.ValidationUtils;
import it.veneto.regione.pagamenti.pivot.ente.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import uk.co.jemos.podam.api.PodamFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LegacyQueryAssessmentsServiceTest {
  @Mock
  private AssessmentService assessmentServiceMock;
  @Mock
  private LegacyAssessmentsBalanceMapper legacyAssessmentsBalanceMapperMock;

  private LegacyQueryAssessmentsService service;

  private UserInfo userInfo;
  private final String orgIpaCode = "ORG_IPA_CODE";

  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  @BeforeEach
  void setUp() {
    service = new LegacyQueryAssessmentsService(assessmentServiceMock, legacyAssessmentsBalanceMapperMock);
    userInfo = AuthorizationServiceTest.buildAdminUser(1L, "ORGFC", orgIpaCode);
  }

  @Test
  void givenUserNotAdminWhenHandlePivotSILChiediAccertamentoThenUnauthorizedException() {
    // Given
    PivotSILChiediAccertamento request = new PivotSILChiediAccertamento();
    RichiestaPerIUF richiestaPerIUF = podamFactory.manufacturePojo(RichiestaPerIUF.class);
    request.setRichiestaPerIUF(richiestaPerIUF);
    String otherOrgIpaCode = "OTHER_ORG_IPA_CODE";
    // When, Then
    assertThrows(AuthorizationDeniedException.class, () ->
      service.handlePivotSILChiediAccertamento(userInfo, "token", otherOrgIpaCode, request)
    );
  }

  @Test
  void givenFullRequestWhenHandlePivotSILChiediAccertamentoThenIllegalArgumentException() {
    // Given
    PivotSILChiediAccertamento request = new PivotSILChiediAccertamento();
    RichiestaPerBolletta richiestaPerBolletta = podamFactory.manufacturePojo(RichiestaPerBolletta.class);
    RichiestaPerIUF richiestaPerIUF = podamFactory.manufacturePojo(RichiestaPerIUF.class);
    request.setRichiestaPerIUF(richiestaPerIUF);
    request.setRichiestaPerBolletta(richiestaPerBolletta);
    try (MockedStatic<ValidationUtils> validationMock = mockStatic(ValidationUtils.class)) {
      validationMock.when(() -> ValidationUtils.verifyExclusivePresence(request.getRichiestaPerBolletta(), request.getRichiestaPerIUF()))
        .thenReturn(false);

      // When, Then
      assertThrows(SilFaultException.class, () ->
        service.handlePivotSILChiediAccertamento(userInfo, "token", orgIpaCode, request)
      );
    }
  }

  @Test
  void givenRichiestaPerIUFWhenHandlePivotSILChiediAccertamentoThenSilFaultException() {
    // Given
    PivotSILChiediAccertamento request = new PivotSILChiediAccertamento();
    RichiestaPerIUF richiestaPerIUF = podamFactory.manufacturePojo(RichiestaPerIUF.class);
    request.setRichiestaPerIUF(richiestaPerIUF);
    try (MockedStatic<ValidationUtils> validationMock = mockStatic(ValidationUtils.class)) {
      validationMock.when(() -> ValidationUtils.verifyExclusivePresence(request.getRichiestaPerBolletta(), request.getRichiestaPerIUF()))
        .thenReturn(true);

      when(assessmentServiceMock.findClosedAssessmentsBalanceViewByOrganizationIdAndIuf(anyLong(), any(), any()))
        .thenReturn(Collections.emptyList());

      // When, Then
      assertThrows(SilFaultException.class, () ->
        service.handlePivotSILChiediAccertamento(userInfo, "token", orgIpaCode, request)
      );
    }
  }

  @ParameterizedTest
  @MethodSource("provideGetAssessmentInput")
  void givenRichiestaPerBollettaWhenHandlePivotSILChiediAccertamentoThenSuccess(RichiestaPerIUF richiestaPerIUF, RichiestaPerBolletta richiestaPerBolletta) {
    // Given
    PivotSILChiediAccertamento request = new PivotSILChiediAccertamento();
    request.setRichiestaPerBolletta(richiestaPerBolletta);
    request.setRichiestaPerIUF(richiestaPerIUF);
    AssessmentsBalanceView assessmentsBalanceView = new AssessmentsBalanceView()
      .officeCode("OFF1")
      .debtPositionTypeOrgCode("TYP1")
      .assessmentCode("CAP1")
      .sectionCode("SEC1")
      .amountCents(12345L);

    CtBilancio expectedCtBilancio = new CtBilancio();

    try (MockedStatic<ValidationUtils> validationMock = mockStatic(ValidationUtils.class)) {
      validationMock.when(() -> ValidationUtils.verifyExclusivePresence(request.getRichiestaPerBolletta(), request.getRichiestaPerIUF()))
        .thenReturn(true);

      if (richiestaPerIUF == null) {
        when(assessmentServiceMock.findClosedAssessmentsBalanceViewByOrganizationIdAndBill(anyLong(), any(), any(), any()))
          .thenReturn(List.of(assessmentsBalanceView));
      } else {
        when(assessmentServiceMock.findClosedAssessmentsBalanceViewByOrganizationIdAndIuf(anyLong(), any(), any()))
          .thenReturn(List.of(assessmentsBalanceView));
      }

      when(legacyAssessmentsBalanceMapperMock.map2CtBilancio(assessmentsBalanceView)).thenReturn(expectedCtBilancio);

      // When
      PivotSILChiediAccertamentoRisposta response = service.handlePivotSILChiediAccertamento(userInfo, "token", orgIpaCode, request);

      // Then
      assertNotNull(response);
      assertEquals(expectedCtBilancio, response.getBilancios().getFirst());
    }
  }

  private static Stream<Arguments> provideGetAssessmentInput() {
    RichiestaPerBolletta billRequest = new RichiestaPerBolletta();
    billRequest.setAnnoBolletta("2024");
    billRequest.setNumeroBolletta("123");
    RichiestaPerIUF iufRequest = new RichiestaPerIUF();
    iufRequest.setIdentificativoUnivocoFlusso("IUF123");
    return Stream.of(
      Arguments.of( null, billRequest),
      Arguments.of(iufRequest, null)
    );
  }
}
