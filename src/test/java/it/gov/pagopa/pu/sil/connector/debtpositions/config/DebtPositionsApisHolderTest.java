package it.gov.pagopa.pu.sil.connector.debtpositions.config;

import it.gov.pagopa.pu.sil.connector.BaseApiHolderTest;
import it.gov.pagopa.pu.sil.exception.DebtPositionsResponseErrorHandler;
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
class DebtPositionsApisHolderTest extends BaseApiHolderTest {
    @Mock
    private RestTemplateBuilder restTemplateBuilderMock;

    private DebtPositionsApisHolder apisHolder;

    @BeforeEach
    void setUp() {
        Mockito.when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);
        Mockito.when(restTemplateMock.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        Mockito.doNothing().when(restTemplateMock).setErrorHandler(Mockito.any(
          DebtPositionsResponseErrorHandler.class));
        DebtPositionsApiClientConfig clientConfig = DebtPositionsApiClientConfig.builder()
          .baseUrl("http://example.com")
          .build();
        apisHolder = new DebtPositionsApisHolder(clientConfig, restTemplateBuilderMock);
    }

    @AfterEach
    void verifyNoMoreInteractions() {
        Mockito.verifyNoMoreInteractions(
                restTemplateBuilderMock,
                restTemplateMock
        );
    }

    @Test
    void whenGetDebtPositionTypeOrgSearchControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
        assertAuthenticationShouldBeSetInThreadSafeMode(
                accessToken -> apisHolder.getDebtPositionTypeOrgSearchControllerApi(accessToken)
                        .crudDebtPositionTypeOrgsFindByOrganizationIdAndCode(1L, "CODE"),
                new ParameterizedTypeReference<>() {},
                apisHolder::unload);
    }

  @Test
  void whenGetDebtPositionTypeEntityControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> apisHolder.getDebtPositionTypeEntityControllerApi(accessToken)
        .crudGetDebtpositiontype("1"),
      new ParameterizedTypeReference<>() {},
      apisHolder::unload);
  }

  @Test
  void whenGetInstallmentApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> apisHolder.getInstallmentApi(accessToken)
        .getInstallmentsByOrganizationIdAndNav(1L, "NAV", null),
      new ParameterizedTypeReference<>() {},
      apisHolder::unload);
  }

  @Test
  void whenGetInstallmentNoPiiSearchControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> apisHolder.getInstallmentNoPiiSearchControllerApi(accessToken)
        .crudInstallmentsIsInstallmentExists(1L, "IUD", "IUV", "NAV", null),
      new ParameterizedTypeReference<>() {},
      apisHolder::unload);
  }

  @Test
  void whenGetDebtPositionApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> apisHolder.getDebtPositionApi(accessToken)
        .getDebtPositionByInstallmentId(1L),
      new ParameterizedTypeReference<>() {},
      apisHolder::unload);
  }

  @Test
  void whenGetDebtPositionEntityControllerApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> apisHolder.getDebtPositionSearchControllerApi(accessToken)
        .crudDebtPositionsFindByInstallmentId(1L),
      new ParameterizedTypeReference<>() {},
      apisHolder::unload);
  }

  @Test
  void whenGetReceiptApiThenAuthenticationShouldBeSetInThreadSafeMode() throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(
      accessToken -> apisHolder.getReceiptApi(accessToken)
        .getReceipt(1L),
      new ParameterizedTypeReference<>() {},
      apisHolder::unload);
  }

}
