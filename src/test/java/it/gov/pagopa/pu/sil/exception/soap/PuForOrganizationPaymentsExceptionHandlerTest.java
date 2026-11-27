package it.gov.pagopa.pu.sil.exception.soap;

import it.gov.pagopa.pu.sil.exception.soap.transcoder.PuForOrganizationPaymentExceptionTranscoder;
import it.veneto.regione.pagamenti.ente.FaultBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PuForOrganizationPaymentsExceptionHandlerTest extends BaseSoapExceptionHandlerTest<FaultBean> {

  @Mock
  private PuForOrganizationPaymentExceptionTranscoder exceptionTranscoderMock;

  private PuForOrganizationPaymentsExceptionHandler exceptionHandler;

  @BeforeEach
  void init(){
    exceptionHandler = new PuForOrganizationPaymentsExceptionHandler(exceptionTranscoderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions(){
    Mockito.verifyNoMoreInteractions(exceptionTranscoderMock);
  }

  @Override
  protected PuForOrganizationPaymentsExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }

  @Override
  protected PuForOrganizationPaymentExceptionTranscoder getExceptionTranscoderMock() {
    return exceptionTranscoderMock;
  }

  @Override
  protected void assertFaultContent(FaultBean result, String expectedFaultId, int expectedSerial, SoapFaultTranscoded expectedTranscode) {
    Assertions.assertEquals(expectedFaultId, result.getId());
    Assertions.assertEquals(expectedSerial, result.getSerial());
    Assertions.assertEquals(expectedTranscode.getSilFault().code(), result.getFaultCode());
    Assertions.assertEquals(expectedTranscode.getSilFault().description(), result.getFaultString());
    Assertions.assertEquals(expectedTranscode.getDescription(), result.getDescription());
  }
}
