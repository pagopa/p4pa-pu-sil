package it.gov.pagopa.pu.sil.connector.pagopapayments.config;

import it.gov.pagopa.pu.sil.config.json.JsonConfig;
import it.gov.pagopa.pu.sil.config.rest.HttpClientErrorJsonBodyHandler;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoPaPaymentsApisHolderTest extends BaseApiHolderTest {
	@Mock
	private RestTemplateBuilder restTemplateBuilderMock;

	private PagoPaPaymentsApisHolder pagoPaPaymentsApisHolder;
  private PagoPaPaymentsApiClientConfig apiClientConfig;

	@BeforeEach
	void setUp() {
		when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
		when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
		apiClientConfig = PagoPaPaymentsApiClientConfig.builder()
				.baseUrl("http://example.com")
      .maxAttempts(3)
				.build();
		pagoPaPaymentsApisHolder = new PagoPaPaymentsApisHolder(apiClientConfig, restTemplateBuilderMock, new JsonConfig().objectMapperJackson3());

    verify(restTemplateMock)
      .setErrorHandler(Mockito.any(HttpClientErrorJsonBodyHandler.class));
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
        pagoPaPaymentsApisHolder.getPrintPaymentNoticeApi(accessToken)
          .getSignedUrl(1L, "folderId"),
      new ParameterizedTypeReference<>() {
      }
    );
  }

	@Test
	void whenGetSignedUrlApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
		assertAuthenticationShouldBeSetInThreadSafeMode(
				accessToken ->
					pagoPaPaymentsApisHolder.getPrintPaymentNoticeApi(accessToken)
            .getSignedUrl(1L, "folderId"),
				new ParameterizedTypeReference<>() {},
				pagoPaPaymentsApisHolder::unload);
	}
}
