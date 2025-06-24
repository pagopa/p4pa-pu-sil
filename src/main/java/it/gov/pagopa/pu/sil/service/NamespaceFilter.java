package it.gov.pagopa.pu.sil.service;

import lombok.extern.slf4j.Slf4j;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

@Slf4j
public class NamespaceFilter extends XMLFilterImpl {

  private String namespace;

  public NamespaceFilter(String namespace) {
    this.namespace = namespace;
  }

  @Override
  public void endElement(String uri, String localName, String qName)
          throws SAXException {
    super.endElement(namespace, localName, qName);
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    if (uri == null || uri.isEmpty()) {
      log.info("Adding namespace to element: {}", localName);
      AttributesImpl newAttrs = new AttributesImpl(atts);
      super.startElement(namespace, localName, qName, newAttrs);
    } else {
      super.startElement(uri, localName, qName, atts);
    }
  }
}
