package it.gov.pagopa.pu.sil.service.soap;

import it.gov.pagopa.pu.sil.exception.ApplicationException;
import it.gov.pagopa.pu.sil.util.ConversionUtils;
import it.gov.pagopa.pu.sil.util.TestUtils;
import it.veneto.regione.pagamenti.ente.PaaSILAutorizzaImportFlusso;
import it.veneto.regione.schemas._2012.pagamenti.ente.*;
import jakarta.xml.bind.UnmarshalException;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.xml.sax.SAXParseException;
import uk.co.jemos.podam.api.PodamFactory;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
  classes = {
    JAXBTransformService.class},
  webEnvironment = SpringBootTest.WebEnvironment.NONE)
class JaxbTransformServiceTest {

  @Autowired
  private JAXBTransformService jaxbTransformService;

  private final PodamFactory podamFactory;

  JaxbTransformServiceTest() {
    this.podamFactory = TestUtils.getPodamFactory();
  }

  private static final String EXPECTED_RESPONSE_TEMPLATE = "<%s%s><password>%s</password></%s>";
  private static final String EXPECTED_NAMESPACE = " xmlns:ns2=\"http://www.regione.veneto.it/pagamenti/ente/\"";

  //region Marshalling

  @Test
  void givenValidObjectWhenMarshallingThenOk() {
    // given
    PaaSILAutorizzaImportFlusso request = podamFactory.manufacturePojo(PaaSILAutorizzaImportFlusso.class);
    String rootElement = "ns2:paaSILAutorizzaImportFlusso";
    String expectedResponse = EXPECTED_RESPONSE_TEMPLATE.formatted(rootElement, EXPECTED_NAMESPACE, request.getPassword(), rootElement);

    // when
    String response = jaxbTransformService.marshalling(request, PaaSILAutorizzaImportFlusso.class);

    // then
    Assertions.assertEquals(expectedResponse, response);
  }

  @Test
  void givenValidObjectWhenMarshallingAsBytesThenOk() {
    // given
    PaaSILAutorizzaImportFlusso request = podamFactory.manufacturePojo(PaaSILAutorizzaImportFlusso.class);
    String rootElement = "ns2:paaSILAutorizzaImportFlusso";
    byte[] expectedResponse = EXPECTED_RESPONSE_TEMPLATE.formatted(rootElement, EXPECTED_NAMESPACE, request.getPassword(), rootElement)
      .getBytes(StandardCharsets.UTF_8);

    // when
    byte[] response = jaxbTransformService.marshallingAsBytes(request, PaaSILAutorizzaImportFlusso.class);

    // then
    Assertions.assertArrayEquals(expectedResponse, response);
  }

  @Test
  void givenValidObjectAsBytesWithJaxbElementNameWhenMarshallingThenOk() {
    // given
    PaaSILAutorizzaImportFlusso request = podamFactory.manufacturePojo(PaaSILAutorizzaImportFlusso.class);
    String rootElement = "XXX";
    byte[] expectedResponse = EXPECTED_RESPONSE_TEMPLATE.formatted(rootElement, EXPECTED_NAMESPACE, request.getPassword(), rootElement)
      .getBytes(StandardCharsets.UTF_8);

    // when
    byte[] response = jaxbTransformService.marshallingAsBytes(request, PaaSILAutorizzaImportFlusso.class, rootElement);

    // then
    Assertions.assertEquals(new String(expectedResponse, StandardCharsets.UTF_8), new String(response, StandardCharsets.UTF_8));
    Assertions.assertArrayEquals(expectedResponse, response);
  }

  @Test
  void givenNullObjectWhenMarshallingThenOk() {
    // given

    // when
    String response = jaxbTransformService.marshalling(null, PaaSILAutorizzaImportFlusso.class);

    // then
    Assertions.assertNull(response);
  }

  @Test
  void givenValidObjectWhenMarshallingNoNamespaceThenOk() {
    // given
    PaaSILAutorizzaImportFlusso request = podamFactory.manufacturePojo(PaaSILAutorizzaImportFlusso.class);
    String rootElement = "ns2:paaSILAutorizzaImportFlusso";
    String expectedResponse = EXPECTED_RESPONSE_TEMPLATE.formatted(rootElement, "", request.getPassword(), rootElement);

    // when
    String response = jaxbTransformService.marshallingNoNamespace(request, PaaSILAutorizzaImportFlusso.class);

    // then
    Assertions.assertEquals(expectedResponse, response);
  }

  //endregion

  //region Unmarshalling

  @Test
  void givenValidXmlWhenUnmarshallingThenOk() {
    // given
    PaaSILAutorizzaImportFlusso expectedResponse = podamFactory.manufacturePojo(PaaSILAutorizzaImportFlusso.class);
    String rootElement = "ns2:paaSILAutorizzaImportFlusso";
    byte[] request = EXPECTED_RESPONSE_TEMPLATE.formatted(rootElement, EXPECTED_NAMESPACE, expectedResponse.getPassword(),
      rootElement).getBytes(StandardCharsets.UTF_8);

    // when
    PaaSILAutorizzaImportFlusso response = jaxbTransformService.unmarshalling(request, PaaSILAutorizzaImportFlusso.class);

    // then
    Assertions.assertTrue(EqualsBuilder.reflectionEquals(expectedResponse, response, true, null, true));
  }

  @Test
  void givenValidXmlWithXsdSchemaWhenUnmarshallingThenOk() throws ParseException {
    Versamento expectedResponse = new Versamento();
    expectedResponse.setAzione("I");
    expectedResponse.setVersioneOggetto("6.0");
    CtSoggettoPagatore soggettoPagatore = new CtSoggettoPagatore();
    CtIdentificativoUnivocoPersonaFG identificativoUnivocoPagatore = new CtIdentificativoUnivocoPersonaFG();
    identificativoUnivocoPagatore.setCodiceIdentificativoUnivoco("TSTTNT80A01H501O");
    identificativoUnivocoPagatore.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.F);
    soggettoPagatore.setIdentificativoUnivocoPagatore(identificativoUnivocoPagatore);
    soggettoPagatore.setAnagraficaPagatore("UTENTE TESTER");
    expectedResponse.setSoggettoPagatore(soggettoPagatore);
    CtDatiVersamento datiVersamento = new CtDatiVersamento();
    datiVersamento.setCausaleVersamento("Restituzione PVA: XXXX");
    datiVersamento.setDatiSpecificiRiscossione("9/---");
    datiVersamento.setIdentificativoTipoDovuto("RESTITUZIONE_FEAGA_FEASR");
    datiVersamento.setImportoSingoloVersamento(BigDecimal.valueOf(0.96));
    datiVersamento.setIdentificativoUnivocoDovuto("99979588_10");
    datiVersamento.setIdentificativoUnivocoVersamento("010312345678901");
    XMLGregorianCalendar xmlGregorianCalendar = ConversionUtils.toXMLGregorianCalendar(
      ConversionUtils.toOffsetDateTime(new SimpleDateFormat("yyyy-MM-dd").parse("2025-07-18")));
    xmlGregorianCalendar.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
    xmlGregorianCalendar.setTime(DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED);
    datiVersamento.setDataEsecuzionePagamento(xmlGregorianCalendar);
    expectedResponse.setDatiVersamento(datiVersamento);

    byte[] request = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Versamento\n" +
      "\txmlns=\"http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/\"><versioneOggetto>6.0</versioneOggetto><soggettoPagatore><identificativoUnivocoPagatore><tipoIdentificativoUnivoco>F</tipoIdentificativoUnivoco><codiceIdentificativoUnivoco>TSTTNT80A01H501O</codiceIdentificativoUnivoco></identificativoUnivocoPagatore><anagraficaPagatore>UTENTE TESTER</anagraficaPagatore></soggettoPagatore><datiVersamento><dataEsecuzionePagamento>2025-07-18</dataEsecuzionePagamento><identificativoUnivocoVersamento>010312345678901</identificativoUnivocoVersamento><identificativoUnivocoDovuto>99979588_10</identificativoUnivocoDovuto><importoSingoloVersamento>0.96</importoSingoloVersamento><identificativoTipoDovuto>RESTITUZIONE_FEAGA_FEASR</identificativoTipoDovuto><causaleVersamento>Restituzione PVA: XXXX</causaleVersamento><datiSpecificiRiscossione>9/---</datiSpecificiRiscossione></datiVersamento><azione>I</azione></Versamento>")
      .getBytes(StandardCharsets.UTF_8);

    // when
    Versamento response = jaxbTransformService.unmarshalling(request, Versamento.class, "/soap/wsdl/payments/PagInf_Dovuti_Pagati_6_2_0.xsd");

    // then
    Assertions.assertTrue(EqualsBuilder.reflectionEquals(expectedResponse, response, true, null, true, "importoSingoloVersamento", "dataEsecuzionePagamento"));
    Assertions.assertEquals(expectedResponse.getDatiVersamento().getImportoSingoloVersamento(), response.getDatiVersamento().getImportoSingoloVersamento());
    Assertions.assertEquals(expectedResponse.getDatiVersamento().getDataEsecuzionePagamento(), response.getDatiVersamento().getDataEsecuzionePagamento());
  }

  @Test
  void givenValidXmlWithInvalidXmlCharWhenUnmarshallingThenOk() throws ParseException {
    Versamento expectedResponse = new Versamento();
    expectedResponse.setAzione("I");
    expectedResponse.setVersioneOggetto("6.0");
    CtSoggettoPagatore soggettoPagatore = new CtSoggettoPagatore();
    CtIdentificativoUnivocoPersonaFG identificativoUnivocoPagatore = new CtIdentificativoUnivocoPersonaFG();
    identificativoUnivocoPagatore.setCodiceIdentificativoUnivoco("TSTTNT80A01H501O");
    identificativoUnivocoPagatore.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.F);
    soggettoPagatore.setIdentificativoUnivocoPagatore(identificativoUnivocoPagatore);
    soggettoPagatore.setAnagraficaPagatore("UTENTE TESTER");
    expectedResponse.setSoggettoPagatore(soggettoPagatore);
    CtDatiVersamento datiVersamento = new CtDatiVersamento();
    datiVersamento.setCausaleVersamento("Restituzione PVA: XXXX");
    datiVersamento.setDatiSpecificiRiscossione("9/---");
    datiVersamento.setIdentificativoTipoDovuto("RESTITUZIONE_FEAGA_FEASR");
    datiVersamento.setImportoSingoloVersamento(BigDecimal.valueOf(0.96));
    datiVersamento.setIdentificativoUnivocoDovuto("99979588_10");
    datiVersamento.setIdentificativoUnivocoVersamento("010312345678901");
    XMLGregorianCalendar xmlGregorianCalendar = ConversionUtils.toXMLGregorianCalendar(
      ConversionUtils.toOffsetDateTime(new SimpleDateFormat("yyyy-MM-dd").parse("2025-07-18")));
    xmlGregorianCalendar.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
    xmlGregorianCalendar.setTime(DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED);
    datiVersamento.setDataEsecuzionePagamento(xmlGregorianCalendar);
    expectedResponse.setDatiVersamento(datiVersamento);

    byte[] request = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Versamento\n" +
      "\txmlns=\"http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/\"><versioneOggetto>6.0</versioneOggetto><soggettoPagatore><identificativoUnivocoPagatore><tipoIdentificativoUnivoco>F</tipoIdentificativoUnivoco><codiceIdentificativoUnivoco>\u0000TSTTNT80A01H501O</codiceIdentificativoUnivoco></identificativoUnivocoPagatore><anagraficaPagatore>UTENTE TESTER</anagraficaPagatore></soggettoPagatore><datiVersamento><dataEsecuzionePagamento>2025-07-18</dataEsecuzionePagamento><identificativoUnivocoVersamento>010312345678901</identificativoUnivocoVersamento><identificativoUnivocoDovuto>99979588_10</identificativoUnivocoDovuto><importoSingoloVersamento>0.96</importoSingoloVersamento><identificativoTipoDovuto>RESTITUZIONE_FEAGA_FEASR</identificativoTipoDovuto><causaleVersamento>Restituzione PVA: XXXX</causaleVersamento><datiSpecificiRiscossione>9/---</datiSpecificiRiscossione></datiVersamento><azione>I</azione></Versamento>")
      .getBytes(StandardCharsets.UTF_8);

    // when
    Versamento response = jaxbTransformService.unmarshalling(request, Versamento.class, "/soap/wsdl/payments/PagInf_Dovuti_Pagati_6_2_0.xsd");

    // then
    Assertions.assertTrue(EqualsBuilder.reflectionEquals(expectedResponse, response, true, null, true, "importoSingoloVersamento", "dataEsecuzionePagamento"));
    Assertions.assertEquals(expectedResponse.getDatiVersamento().getImportoSingoloVersamento(), response.getDatiVersamento().getImportoSingoloVersamento());
    Assertions.assertEquals(expectedResponse.getDatiVersamento().getDataEsecuzionePagamento(), response.getDatiVersamento().getDataEsecuzionePagamento());
  }


  @Test
  void givenNullObjectWhenUnmarshallingThenOk() {
    // given

    // when
    PaaSILAutorizzaImportFlusso response = jaxbTransformService.unmarshalling(null, PaaSILAutorizzaImportFlusso.class);

    // then
    Assertions.assertNull(response);
  }

  //endregion

  //region: getDetailUnmarshalExceptionMessage
  @Test
  void getDetailUnmarshalExceptionMessage_WithSAXParseException_ReturnsDetailedMessage() {
    // given
    SAXParseException saxParseException = new SAXParseException("Invalid character", null, null, 2, 33);
    ApplicationException applicationException = new ApplicationException(new UnmarshalException("Unmarshal error", saxParseException));
    byte[] xml = ("""
      <root>
        <element>Invalid\u000Char</element>
      </root>""").getBytes();

    // when
    String result = jaxbTransformService.getDetailUnmarshalExceptionMessage(applicationException, xml);

    // then
    assertEquals("element: element - Invalid character", result);
  }

  @Test
  void getDetailUnmarshalExceptionMessage_WithSAXParseExceptionButNoField_ReturnsMessage() {
    // given
    SAXParseException saxParseException = new SAXParseException("Invalid character", null, null, 2, 1);
    ApplicationException applicationException = new ApplicationException(new UnmarshalException("Unmarshal error", saxParseException));
    byte[] xml = ("""
      <root>
        <>
      </root>""").getBytes();

    // when
    String result = jaxbTransformService.getDetailUnmarshalExceptionMessage(applicationException, xml);

    // then
    assertEquals("Invalid character", result);
  }

  @Test
  void getDetailUnmarshalExceptionMessage_WithNoSAXParseException_ReturnsFallbackMessage() {
    // given
    ApplicationException applicationException = new ApplicationException(new UnmarshalException("Unmarshal error"));
    byte[] xml = "<root></root>".getBytes();

    // when
    String result = jaxbTransformService.getDetailUnmarshalExceptionMessage(applicationException, xml);

    // then
    assertEquals("Unmarshal error", result);
  }

  @Test
  void getDetailUnmarshalExceptionMessage_WithNullException_ReturnsNull() {
    // when
    String result = jaxbTransformService.getDetailUnmarshalExceptionMessage(null, null);

    // then
    assertEquals("null exception", result);
  }

  @Test
  void getDetailUnmarshalExceptionMessage_WithErrorHandling_ReturnsFallbackMessage() {
    // given
    ApplicationException applicationException = Mockito.mock(ApplicationException.class);
    Mockito.when(applicationException.getCause()).thenThrow(new RuntimeException("Unexpected error"));
    Mockito.when(applicationException.getMessage()).thenReturn("exception message");
    byte[] xml = "<root></root>".getBytes();

    // when
    String result = jaxbTransformService.getDetailUnmarshalExceptionMessage(applicationException, xml);

    // then
    assertEquals("exception message", result);
  }
  //endregion
}
