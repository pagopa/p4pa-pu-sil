package it.gov.pagopa.pu.sil.service.notice;

import it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO;
import it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus;
import it.gov.pagopa.pu.sil.connector.debtpositions.DebtPositionService;
import it.gov.pagopa.pu.sil.connector.pagopapayments.PagopaPaymentsService;
import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.exception.PaymentNotFoundException;
import it.gov.pagopa.pu.sil.util.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import uk.co.jemos.podam.api.PodamFactory;

import java.io.IOException;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

  @Mock
  private DebtPositionService debtPositionServiceMock;
  @Mock
  private PagopaPaymentsService pagopaPaymentsServiceMock;
  @InjectMocks
  private NoticeService noticeService;

  private final String accessToken = "accessToken";
  private final PodamFactory podamFactory = TestUtils.getPodamFactory();

  //region: generateNotice
  @Test
  void whenGenerateNoticeThenOk() {
    String iuv = "IUV123";
    DebtPositionDTO debtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    byte[] pdfContent = "fakePDFContent".getBytes();

    //given
    Mockito.when(pagopaPaymentsServiceMock.generateNotice(iuv, debtPositionDTO, accessToken)).thenReturn(new ByteArrayResource(pdfContent));

    //when
    byte[] response = noticeService.generateNotice(iuv, debtPositionDTO, accessToken);

    //verify
    Assertions.assertArrayEquals(pdfContent, response);
  }

  @Test
  void givenNotFoundNoticeWhenGenerateNoticeThenException() {
    String iuv = "IUV123";
    DebtPositionDTO debtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);

    //given
    Mockito.when(pagopaPaymentsServiceMock.generateNotice(iuv, debtPositionDTO, accessToken)).thenReturn(null);

    //when
    ApplicationException exception = Assertions.assertThrows(ApplicationException.class, ()-> noticeService.generateNotice(iuv, debtPositionDTO, accessToken));

    //verify
    Assertions.assertTrue(exception.getMessage().startsWith("notice not found for org"));
  }

  @Test
  void givenIoExceptionWhenGenerateNoticeThenException() throws IOException {
    String iuv = "IUV123";
    DebtPositionDTO debtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    Resource pdfResource = Mockito.mock(ByteArrayResource.class);
    IOException ioException = new IOException("IO error");
    Mockito.when(pdfResource.getContentAsByteArray()).thenThrow(ioException);

    //given
    Mockito.when(pagopaPaymentsServiceMock.generateNotice(iuv, debtPositionDTO, accessToken)).thenReturn(pdfResource);

    //when
    ApplicationException exception = Assertions.assertThrows(ApplicationException.class, ()-> noticeService.generateNotice(iuv, debtPositionDTO, accessToken));

    //verify
    Assertions.assertEquals(ioException, exception.getCause());
  }
  //endregion

  //region: generateNoticeByIuv
  @Test
  void whenGenerateNoticeByIuvThenOk() {
    Long organizationId = 123L;
    String iuv = "IUV"+System.currentTimeMillis();
    DebtPositionDTO debtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    debtPositionDTO.setOrganizationId(organizationId);
    InstallmentDTO installmentDTO = debtPositionDTO.getPaymentOptions().getLast().getInstallments().getLast();
    installmentDTO.setIuv(iuv);
    installmentDTO.setStatus(InstallmentStatus.UNPAID);

    Resource pdfResource = new ByteArrayResource("fakePDFContent".getBytes());

    //given
    Mockito.when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIuv(Mockito.eq(organizationId), Mockito.eq(iuv), Mockito.anyList(), Mockito.eq(accessToken)))
        .thenReturn(List.of(debtPositionDTO));
    Mockito.when(pagopaPaymentsServiceMock.generateNotice(iuv, debtPositionDTO, accessToken)).thenReturn(pdfResource);

    //when
    Resource response = noticeService.generateNoticeByIuv(organizationId, iuv, accessToken);

    //verify
    Assertions.assertEquals(pdfResource, response);
  }

  @Test
  void givenNotFoundWhenGenerateNoticeByIuvThenException() {
    Long organizationId = 123L;
    String iuv = "IUV"+System.currentTimeMillis();
    DebtPositionDTO debtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);

    //given
    Mockito.when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIuv(Mockito.eq(organizationId), Mockito.eq(iuv), Mockito.anyList(), Mockito.eq(accessToken)))
      .thenReturn(List.of(debtPositionDTO));

    //when
    PaymentNotFoundException exception = Assertions.assertThrows(PaymentNotFoundException.class, () -> noticeService.generateNoticeByIuv(organizationId, iuv, accessToken));

    //verify
    Assertions.assertTrue(exception.getMessage().startsWith("No installment found for IUV"));
  }

  @Test
  void givenNullNoticeWhenGenerateNoticeByIuvThenException() {
    Long organizationId = 123L;
    String iuv = "IUV"+System.currentTimeMillis();
    DebtPositionDTO debtPositionDTO = podamFactory.manufacturePojo(DebtPositionDTO.class);
    debtPositionDTO.setOrganizationId(organizationId);
    InstallmentDTO installmentDTO = debtPositionDTO.getPaymentOptions().getLast().getInstallments().getLast();
    installmentDTO.setIuv(iuv);
    installmentDTO.setStatus(InstallmentStatus.UNPAID);

    //given
    Mockito.when(debtPositionServiceMock.getDebtPositionsByOrganizationIdAndIuv(Mockito.eq(organizationId), Mockito.eq(iuv), Mockito.anyList(), Mockito.eq(accessToken)))
      .thenReturn(List.of(debtPositionDTO));
    Mockito.when(pagopaPaymentsServiceMock.generateNotice(iuv, debtPositionDTO, accessToken)).thenReturn(null);

    //when
    ApplicationException exception = Assertions.assertThrows(ApplicationException.class, () -> noticeService.generateNoticeByIuv(organizationId, iuv, accessToken));

    //verify
    Assertions.assertTrue(exception.getMessage().startsWith("Error generating notice for IUV"));
  }
  //endregion
}
