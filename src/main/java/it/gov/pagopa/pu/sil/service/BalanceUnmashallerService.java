package it.gov.pagopa.pu.sil.service;

import it.veneto.regione.schemas._2012.pagamenti.ente.Bilancio;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;

@Slf4j
@Component
public class BalanceUnmashallerService {
  private final JAXBContext jaxbContext;
  private final Schema schema;
  private final XMLUnmarshallerService xmlUnmarshallerService;

  private static final String NAMESPACE = "http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/";

  public BalanceUnmashallerService(@Value("classpath:soap/wsdl/payments/PagInf_Dovuti_Pagati_6_2_0.xsd") Resource paymetsReportingXsdResource,
                                   XMLUnmarshallerService xmlUnmarshallerService) {
    try {
      this.jaxbContext = JAXBContext.newInstance(Bilancio.class);
      SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      this.schema = schemaFactory.newSchema(paymetsReportingXsdResource.getURL());
      this.xmlUnmarshallerService = xmlUnmarshallerService;
    } catch (JAXBException | SAXException | IOException e) {
      throw new IllegalStateException("Error while creating jaxb context for CtBilancio", e);
    }
  }

  /**
   * Unmarshals a file into a CtFlussoRiversamento object.
   *
   * @param xmlString the XML string to parse
   * @return the unmarshalled CtFlussoRiversamento object
   */
  public Bilancio unmarshal(String xmlString) {
    return xmlUnmarshallerService.unmarshal(xmlString, Bilancio.class, jaxbContext, schema, NAMESPACE);
  }
}
