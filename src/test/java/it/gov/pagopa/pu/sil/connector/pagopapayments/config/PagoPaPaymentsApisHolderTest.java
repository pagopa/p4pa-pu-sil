package it.gov.pagopa.pu.sil.connector.pagopapayments.config;

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

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoPaPaymentsApisHolderTest extends BaseApiHolderTest {
	@Mock
	private RestTemplateBuilder restTemplateBuilderMock;

	private PagoPaPaymentsApisHolder pagoPaPaymentsApisHolder;

	@BeforeEach
	void setUp() {
		when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
		when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
		PagoPaPaymentsApiClientConfig clientConfig = PagoPaPaymentsApiClientConfig.builder()
				.baseUrl("http://example.com")
				.build();
		pagoPaPaymentsApisHolder = new PagoPaPaymentsApisHolder(clientConfig, restTemplateBuilderMock);
	}

	@AfterEach
	void tearDown() {
		Mockito.verifyNoMoreInteractions(
			restTemplateBuilderMock,
			restTemplateMock
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
