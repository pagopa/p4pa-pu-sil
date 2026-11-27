package it.gov.pagopa.pu.sil.connector.pagopapayments.config;

import it.gov.pagopa.pu.sil.config.json.JsonConfig;
import it.gov.pagopa.pu.sil.connector.BaseApiHolderTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoPaPaymentsApisHolderTest extends BaseApiHolderTest {
	@Mock
	private RestTemplateBuilder restTemplateBuilderMock;

	private PagoPaPaymentsApisHolder apisHolder;
  private PagoPaPaymentsApiClientConfig apiClientConfig;

	@BeforeEach
	void setUp() {
		when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
		when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());

    apiClientConfig = PagoPaPaymentsApiClientConfig.builder()
				.baseUrl("http://example.com")
      .maxAttempts(3)
				.build();
		apisHolder = new PagoPaPaymentsApisHolder(apiClientConfig, restTemplateBuilderMock, new JsonConfig().objectMapperJackson3());

    verifyHttpClientErrorJsonBodyHandlerConfiguration(apisHolder.getPrintPaymentNoticeApi(null));
	}

	@AfterEach
	void tearDown() {
		Mockito.verifyNoMoreInteractions(
			restTemplateBuilderMock,
			restTemplateMock
		);
	}

  @Test
  void testRetryConfiguration() {
    assertRetry(apiClientConfig,
      accessToken ->
        apisHolder.getPrintPaymentNoticeApi(accessToken)
          .getSignedUrl(1L, "folderId"),
      new ParameterizedTypeReference<>() {
      }
    );
  }

	@Test
	void whenGetSignedUrlApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
		assertAuthenticationShouldBeSetInThreadSafeMode(
				accessToken ->
					apisHolder.getPrintPaymentNoticeApi(accessToken)
            .getSignedUrl(1L, "folderId"),
				new ParameterizedTypeReference<>() {},
				apisHolder::unload);
	}
}
