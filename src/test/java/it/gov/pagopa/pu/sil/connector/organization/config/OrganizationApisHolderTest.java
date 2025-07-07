package it.gov.pagopa.pu.sil.connector.organization.config;

import it.gov.pagopa.pu.sil.connector.BaseApiHolderTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.util.DefaultUriBuilderFactory;

@ExtendWith(MockitoExtension.class)
class OrganizationApisHolderTest extends BaseApiHolderTest {
  @Mock
  private RestTemplateBuilder restTemplateBuilderMock;

  private OrganizationApisHolder organizationApisHolder;

  @BeforeEach
  void setUp() {
    Mockito.when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
    Mockito.when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
    OrganizationApiClientConfig clientConfig = OrganizationApiClientConfig.builder()
      .baseUrl("http://example.com")
      .build();
    organizationApisHolder = new OrganizationApisHolder(clientConfig, restTemplateBuilderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      restTemplateBuilderMock,
      restTemplateMock
    );
  }

  @Test
  void whenGetOrganizationSearchControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> organizationApisHolder.getOrganizationSearchControllerApi(accessToken)
        .crudOrganizationsFindByIpaCode("ORGIPACODE"),
      new ParameterizedTypeReference<>() {
      },
      organizationApisHolder::unload);
  }

  @Test
  void whenGetOrganizationEntityControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> organizationApisHolder.getOrganizationEntityControllerApi(accessToken)
        .crudGetOrganization("ORGID"),
      new ParameterizedTypeReference<>() {
      },
      organizationApisHolder::unload);
  }

  @Test
  void whenGetBrokerEntityControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> organizationApisHolder.getBrokerEntityControllerApi(accessToken)
        .crudGetBroker("BROKER_ID"),
      new ParameterizedTypeReference<>() {
      },
      organizationApisHolder::unload);
  }

  @Test
  void whenGetBrokerSearchControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> organizationApisHolder.getBrokerSearchControllerApi(accessToken)
        .crudBrokersFindByBrokeredOrganizationId("ORGID"),
      new ParameterizedTypeReference<>() {
      },
      organizationApisHolder::unload);
  }

  @Test
  void whenGetOrgSilServiceEntityControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> organizationApisHolder.getOrganizationSilServiceApi(accessToken)
        .getOrgSilService(0L),
      new ParameterizedTypeReference<>() {
      },
      organizationApisHolder::unload);
  }
}
