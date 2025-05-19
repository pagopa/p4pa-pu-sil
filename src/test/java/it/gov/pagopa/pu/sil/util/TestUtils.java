package it.gov.pagopa.pu.sil.util;

import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPMessage;
import org.junit.jupiter.api.Assertions;
import org.springframework.util.ReflectionUtils;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import uk.co.jemos.podam.api.*;
import uk.co.jemos.podam.common.ManufacturingContext;
import uk.co.jemos.podam.typeManufacturers.AbstractTypeManufacturer;

import javax.xml.datatype.XMLGregorianCalendar;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.*;

public class TestUtils {

  public static PodamFactory getPodamFactory() {
    PodamFactory externalFactory = new AbstractExternalFactory() {
      @Override
      public <T> T manufacturePojo(Class<T> pojoClass, Type... genericTypeArgs) {
        if(pojoClass.isAssignableFrom(XMLGregorianCalendar.class)) {
          return (T) ConversionUtils.toXMLGregorianCalendar(OffsetDateTime.now());
        }
        return null;
      }

      @Override
      public <T> T populatePojo(T pojo, Type... genericTypeArgs) {
        return null;
      }
    };
    PodamFactoryImpl podamFactory = new PodamFactoryImpl(externalFactory);
    podamFactory.getStrategy().addOrReplaceTypeManufacturer(SortedSet.class, new AbstractTypeManufacturer<>(){
      @Override
      public SortedSet<?> getType(DataProviderStrategy strategy, AttributeMetadata attributeMetadata, ManufacturingContext manufacturingCtx) {
        return new TreeSet<>();
      }
    });
    return podamFactory;
  }

  /**
   * It will assert not null on all o's fields
   */
  public static void checkNotNullFields(Object o, String... excludedFields) {
    Set<String> excludedFieldsSet = new HashSet<>(Arrays.asList(excludedFields));
    ReflectionUtils.doWithFields(o.getClass(),
      f -> {
        f.setAccessible(true);
        Assertions.assertNotNull(f.get(o), "The field "+f.getName()+" of the input object of type "+o.getClass()+" is null!");
      },
      f -> !excludedFieldsSet.contains(f.getName()));
  }

  public static void checkNotNullFieldsUsingNullableAnnotation(Object o, String... excludedFields) {
    Set<String> excludedFieldsSet = new HashSet<>(Arrays.asList(excludedFields));
    ReflectionUtils.doWithFields(o.getClass(),
      f -> {
        f.setAccessible(true);
        Assertions.assertNotNull(f.get(o), "The field "+f.getName()+" of the input object of type "+o.getClass()+" is null!");
      },
      f -> !excludedFieldsSet.contains(f.getName()) && f.getAnnotation(Nullable.class)==null);
  }

  public static <T> SoapHeaderElement createSoapHeaderElement(Object headerContent, Class<T> headerType) throws Exception {
    // Marshal the header object into XML
    JAXBContext jaxbContext = JAXBContext.newInstance(headerType);
    Marshaller marshaller = jaxbContext.createMarshaller();

    // Create a SOAPMessage
    MessageFactory messageFactory = MessageFactory.newInstance();
    SOAPMessage soapMessage = messageFactory.createMessage();

    // Retrieve or create the SOAPHeader
    SOAPHeader soapHeader = soapMessage.getSOAPHeader();
    if (soapHeader == null) {
      soapHeader = soapMessage.getSOAPPart().getEnvelope().addHeader();
    }

    // Marshal the IntestazionePPT object into the SOAPHeader
    marshaller.marshal(headerContent, soapHeader);

    // Wrap the SOAPMessage as a SaajSoapMessage
    SaajSoapMessage saajSoapMessage = new SaajSoapMessage(soapMessage);

    // Return the SoapHeaderElement
    return saajSoapMessage.getSoapHeader().examineAllHeaderElements().next();
  }

}
