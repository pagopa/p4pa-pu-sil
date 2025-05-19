package it.gov.pagopa.pu.sil.config;

import it.gov.pagopa.pu.sil.endpoint.PuForOrganizationPayEndpoint;
import it.gov.pagopa.pu.sil.security.WebSecurityConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.support.WebUtils;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.transport.http.WsdlDefinitionHandlerAdapter;
import org.springframework.ws.wsdl.WsdlDefinition;
import org.springframework.ws.wsdl.wsdl11.SimpleWsdl11Definition;
import org.springframework.ws.wsdl.wsdl11.Wsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;
import org.springframework.xml.xsd.XsdSchemaCollection;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EnableWs
@Slf4j
@Configuration(proxyBeanMethods = false)
public class SoapWebServiceConfig extends WsConfigurerAdapter {

  public static final String WS_PATH_PAY = WebSecurityConfig.SOAP_WS_BASE_PATH+"/pay/";
  private static final String SOAP_RESOURCES_FOLDER = "soap";

  public static final String XSD_DOVUTI_PAGATI = "PagInf_Dovuti_Pagati_6_2_0";

  protected static final Set<String> WS_PATH_NAME_SET = new HashSet<>();

  public static final Map<String, String> XSD_NAME_PATH_MAP = Map.of(
    XSD_DOVUTI_PAGATI, "wsdl/"
  );

  private final String silWsdlBaseUrl;
  private final ResourceLoader resourceLoader;

  public SoapWebServiceConfig(
    @Value("${soap.sil.wsdl-base-url}")String silWsdlBaseUrl,
    ResourceLoader resourceLoader) {
    this.silWsdlBaseUrl = silWsdlBaseUrl;
    this.resourceLoader = resourceLoader;
  }

  @Bean
  public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
    MessageDispatcherServlet servlet = new MessageDispatcherServlet(){
      @Override
      protected WsdlDefinition getWsdlDefinition(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String name = WebUtils.extractFilenameFromUrlPath(uri);
        String path = extractPathFromUrlPath(uri);
        log.trace("getWsdlDefinition uri:{} path:{} name:{} found:{}", uri, name, path, WS_PATH_NAME_SET.contains(path+name));
        if(WS_PATH_NAME_SET.contains(path+name))
          return super.getWsdlDefinition(request);
        else
          return null;
      }

      @Override
      protected XsdSchema getXsdSchema(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String name = WebUtils.extractFilenameFromUrlPath(uri);
        String path = extractPathFromUrlPath(uri);
        String xsdPath = request.getContextPath()+ WS_PATH_PAY + XSD_NAME_PATH_MAP.getOrDefault(name,"__NOT_FOUND__");
        if(xsdPath.equals(path))
          return super.getXsdSchema(request);
        else
          return null;
      }
    };
    servlet.setApplicationContext(applicationContext);
    servlet.setTransformWsdlLocations(true);
    return new ServletRegistrationBean<>(servlet, WebSecurityConfig.SOAP_WS_BASE_PATH + "/*");
  }

  private void registerWsdlDefinition(String path){
    log.debug("register ws soap: {}", path);
    WS_PATH_NAME_SET.add(path);
    log.trace("WS_PATH_NAME_SET contains now: {}",WS_PATH_NAME_SET);
  }

  @Bean("wsdlDefinitionHandlerAdapter")
  public WsdlDefinitionHandlerAdapter getWsdlDefinitionHandlerAdapter(){
    return new WsdlDefinitionHandlerAdapter(){
      @Override
      protected String transformLocation(String location, HttpServletRequest request) {
        //do not take url from request, because it may be changed by proxy / ingress. Use application property
        StringBuilder url = new StringBuilder(silWsdlBaseUrl);
        if (location.startsWith("/")) {
          url.append(location);
          return url.toString();
        } else {
          log.error("wsdl url in location must start with / : [{}]", request.getRequestURL());
          return super.transformLocation(location, request);
        }
      }
    };
  }

  private static String extractPathFromUrlPath(String urlPath) {
    int end = urlPath.indexOf('?');
    if (end == -1) {
      end = urlPath.indexOf('#');
      if (end == -1) {
        end = urlPath.length();
      }
    }
    int begin = urlPath.lastIndexOf('/', end) + 1;
    return urlPath.substring(0, begin);
  }

  @Bean
  public XsdSchemaCollection getXsdSchemaCollection() {
    return null;
  }

  @Bean(name = "bean"+PuForOrganizationPayEndpoint.NAME)
  public Wsdl11Definition paForNodeEndpoint(XsdSchemaCollection xsdSchemaCollection) {
    registerWsdlDefinition(WS_PATH_PAY + "wsdl/" + PuForOrganizationPayEndpoint.NAME);
    return new SimpleWsdl11Definition(resourceLoader.getResource("classpath:"+SOAP_RESOURCES_FOLDER+"/wsdl/puForOrganization-pay.wsdl"));
  }

  @Bean(name = "beanPagInfDovutiPagati620")
  public XsdSchema getPaForOrganizationXsd() {
    return new SimpleXsdSchema(new ClassPathResource(SOAP_RESOURCES_FOLDER+"/"+XSD_NAME_PATH_MAP.get(XSD_DOVUTI_PAGATI)+ XSD_DOVUTI_PAGATI +".xsd"));
  }

}
