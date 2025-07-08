package it.gov.pagopa.pu.sil.connector.classification.config;

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

import java.util.List;

@ExtendWith(MockitoExtension.class)
class ClassificationApisHolderTest extends BaseApiHolderTest {
  @Mock
  private RestTemplateBuilder restTemplateBuilderMock;

  private ClassificationApisHolder apisHolder;

  @BeforeEach
  void setUp() {
    Mockito.when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
    Mockito.when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
    ClassificationApiClientConfig clientConfig = ClassificationApiClientConfig.builder()
        .baseUrl("http://example.com")
        .build();
    apisHolder = new ClassificationApisHolder(clientConfig, restTemplateBuilderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
        restTemplateBuilderMock,
        restTemplateMock
    );
  }

  @Test
  void whenGetTreasurySearchControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
        accessToken -> apisHolder.getTreasurySearchControllerApi(accessToken)
            .crudTreasuryFindBySemanticKey(1L, "BILL_CODE", "2025"),
        new ParameterizedTypeReference<>() {},
        apisHolder::unload);
  }

  @Test
  void whenGetPaymentsReportingSearchControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
        accessToken -> apisHolder.getPaymentsReportingSearchControllerApi(accessToken)
            .crudPaymentsReportingFindByOrganizationIdAndIuf(1L, "IUF"),
        new ParameterizedTypeReference<>() {},
        apisHolder::unload);
  }

  @Test
  void whenGetAssessmentsBalanceViewSearchControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
        accessToken -> apisHolder.getAssessmentsBalanceViewSearchControllerApi(accessToken)
            .crudAssessmentsBalanceViewFindClosedByOrganizationIdAndIuds("1", List.of("IUD1", "IUD2")),
        new ParameterizedTypeReference<>() {},
        apisHolder::unload);
  }
}
