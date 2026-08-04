package it.gov.pagopa.pu.sil.connector.send_notification.config;

import it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest;
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
class SendNotificationApisHolderTest  extends BaseApiHolderTest {
  @Mock
  private RestTemplateBuilder restTemplateBuilderMock;

  private SendNotificationApisHolder sendNotificationApisHolder;
  private SendNotificationApiClientConfig apiClientConfig;

  @BeforeEach
  void setUp() {
    when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
    when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
    apiClientConfig = SendNotificationApiClientConfig.builder()
      .baseUrl("http://example.com")
      .maxAttempts(3)
      .build();
    sendNotificationApisHolder = new SendNotificationApisHolder(apiClientConfig, restTemplateBuilderMock, new JsonConfig().objectMapperJackson3());

    verify(restTemplateMock)
      .setErrorHandler(Mockito.any(HttpClientErrorJsonBodyHandler.class));
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      restTemplateBuilderMock,
      restTemplateMock
    );
  }

  @Test
  void testRetryConfiguration() {
    assertRetry(apiClientConfig,
      accessToken -> sendNotificationApisHolder.getNotificationApi(accessToken)
        .createSendNotification(new CreateNotificationRequest()),
      new ParameterizedTypeReference<>() {
      }
    );
  }

  @Test
  void whenGetNotificationApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> sendNotificationApisHolder.getNotificationApi(accessToken)
        .createSendNotification(new CreateNotificationRequest()),
      new ParameterizedTypeReference<>() {},
      sendNotificationApisHolder::unload
    );
  }

  @Test
  void whenGetSendApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> sendNotificationApisHolder.getSendApi(accessToken)
        .retrieveLegalFacts("SEND_NOTIFICATION_ID"),
      new ParameterizedTypeReference<>() {},
      sendNotificationApisHolder::unload
    );
  }
}
