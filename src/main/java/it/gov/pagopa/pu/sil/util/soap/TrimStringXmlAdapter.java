package it.gov.pagopa.pu.sil.util.soap;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import lombok.extern.slf4j.Slf4j;

/*
 * Utility class used to trim all strings during marshalling/unmarshalling of SOAP messages.
 * It is referenced in the .xjb files of single WSDL.
 *
 */
@Slf4j
public class TrimStringXmlAdapter extends XmlAdapter<String, String> {

  @Override
  public String marshal(String text) {
    return this.trim("marshal", text);
  }

  @Override
  public String unmarshal(String v) {
    return this.trim("unmarshal", v);
  }

  private String trim(String oper, String text){
    if(text==null)
      return null;
    String trimmed = text.trim();
    if(log.isDebugEnabled() && trimmed.length()!=text.length())
      log.debug("{}: trim [{}] -> [{}]", oper, text, trimmed);
    return trimmed;
  }
}
