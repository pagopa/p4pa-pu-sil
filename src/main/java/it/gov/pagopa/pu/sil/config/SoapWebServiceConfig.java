package it.gov.pagopa.pu.sil.config;

import it.gov.pagopa.pu.sil.endpoint.PuForOrganizationPaymentsEndpoint;
import it.gov.pagopa.pu.sil.endpoint.PuForOrganizationReconciliationEndpoint;
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

  public static final String WS_PATH_PAYMENTS = WebSecurityConfig.SOAP_WS_BASE_PATH+"/payments/";
  public static final String WS_PATH_RECONCILIATION = WebSecurityConfig.SOAP_WS_BASE_PATH+"/reconciliation/";
  private static final String SOAP_RESOURCES_FOLDER = "soap/wsdl/";

  private static final String WSDL_PAYMENTS = "payments/";
  private static final String WSDL_RECONCILIATION = "reconciliation/";

  public static final String XSD_DOVUTI_PAGATI = "PagInf_Dovuti_Pagati_6_2_0";
  public static final String XSD_PAG_INF_RP_ESITO_6_0_2 = "PagInf_RP_Esito_6_0_2";
  public static final String XSD_FLUSSO_RIVERSAMENTO_1_0_4 = "FlussoRiversamento_1_0_4";

  protected static final Set<String> WS_PATH_NAME_SET = new HashSet<>();

  public static final Map<String, String> XSD_NAME_PATH_MAP = Map.of(
    XSD_DOVUTI_PAGATI, WSDL_PAYMENTS,
    XSD_PAG_INF_RP_ESITO_6_0_2, WSDL_RECONCILIATION,
    XSD_FLUSSO_RIVERSAMENTO_1_0_4, WSDL_RECONCILIATION
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

      @SuppressWarnings("squid:S1075") // Suppressing Hard coded path delimiter: it's a URL, not a file location
      @Override
      protected XsdSchema getXsdSchema(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String name = WebUtils.extractFilenameFromUrlPath(uri);
        String path = extractPathFromUrlPath(uri);
        String xsdPath = request.getContextPath()+ WebSecurityConfig.SOAP_WS_BASE_PATH + "/" +XSD_NAME_PATH_MAP.getOrDefault(name,"__NOT_FOUND__");
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

  @SuppressWarnings("squid:S6830") // Suppressing bean camelCase naming: this is required to match with the service name
  @Bean(name = PuForOrganizationPaymentsEndpoint.NAME)
  public Wsdl11Definition puForOrganizationPaymentsEndpoint(XsdSchemaCollection xsdSchemaCollection) {
    registerWsdlDefinition(WS_PATH_PAYMENTS + PuForOrganizationPaymentsEndpoint.NAME);
    return new SimpleWsdl11Definition(resourceLoader.getResource("classpath:"+SOAP_RESOURCES_FOLDER+"payments/puForOrganization-payments.wsdl"));
  }

  @SuppressWarnings("squid:S6830") // Suppressing bean camelCase naming: this is required to match with the service name
  @Bean(name = PuForOrganizationReconciliationEndpoint.NAME)
  public Wsdl11Definition puForOrganizationReconciliationEndpoint(XsdSchemaCollection xsdSchemaCollection) {
    registerWsdlDefinition(WS_PATH_RECONCILIATION + PuForOrganizationReconciliationEndpoint.NAME);
    return new SimpleWsdl11Definition(resourceLoader.getResource("classpath:"+SOAP_RESOURCES_FOLDER+"reconciliation/puForOrganization-reconciliation.wsdl"));
  }

  @SuppressWarnings("squid:S6830") // Suppressing bean camelCase naming: this is required to match with the service name
  @Bean(name = XSD_DOVUTI_PAGATI)
  public XsdSchema getPaForOrganizationXsd() {
    return new SimpleXsdSchema(new ClassPathResource(SOAP_RESOURCES_FOLDER+XSD_NAME_PATH_MAP.get(XSD_DOVUTI_PAGATI)+ XSD_DOVUTI_PAGATI +".xsd"));
  }

  @SuppressWarnings("squid:S6830") // Suppressing bean camelCase naming: this is required to match with the service name
  @Bean(name = XSD_FLUSSO_RIVERSAMENTO_1_0_4)
  public XsdSchema getFlussoRiversamento104Xsd() {
    return new SimpleXsdSchema(new ClassPathResource(SOAP_RESOURCES_FOLDER+XSD_NAME_PATH_MAP.get(XSD_FLUSSO_RIVERSAMENTO_1_0_4)+ XSD_FLUSSO_RIVERSAMENTO_1_0_4 +".xsd"));
  }

  @SuppressWarnings("squid:S6830") // Suppressing bean camelCase naming: this is required to match with the service name
  @Bean(name = XSD_PAG_INF_RP_ESITO_6_0_2)
  public XsdSchema getPagInfRPEsito602Xsd() {
    return new SimpleXsdSchema(new ClassPathResource(SOAP_RESOURCES_FOLDER+XSD_NAME_PATH_MAP.get(XSD_PAG_INF_RP_ESITO_6_0_2)+ XSD_PAG_INF_RP_ESITO_6_0_2 +".xsd"));
  }
}
