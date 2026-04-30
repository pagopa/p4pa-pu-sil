package it.gov.pagopa.pu.sil.connector;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.IntStream;

public abstract class BaseApiHolderTest {

  public enum AUTH_TYPE {
    NO_AUTH,
    BEARER
  }

  @Mock
  protected RestTemplate restTemplateMock;
  @Mock
  protected Void voidMock;

  protected <T> void assertAuthenticationShouldBeSetInThreadSafeMode(Function<String, T> apiInvoke, ParameterizedTypeReference<T> apiReturnedType, Runnable apiUnloader) throws InterruptedException {
    assertAuthenticationShouldBeSetInThreadSafeMode(apiInvoke, apiReturnedType, apiUnloader, AUTH_TYPE.BEARER);
  }

  @SuppressWarnings("unchecked")
  protected <T> void assertAuthenticationShouldBeSetInThreadSafeMode(Function<String, T> apiInvoke, ParameterizedTypeReference<T> apiReturnedType, Runnable apiUnloader, AUTH_TYPE authType) throws InterruptedException {
    List<Pair<String, T>> useCases;
    if (authType == AUTH_TYPE.BEARER) {
      useCases = IntStream.rangeClosed(0, 100)
        .mapToObj(i -> {
          try {
            String accessToken = "accessToken" + i;
            T expectedResult =
              String.class.equals(apiReturnedType.getType()) ? (T)"RESULT"
              : Integer.class.equals(apiReturnedType.getType()) ? (T)Integer.valueOf(0)
              : Boolean.class.equals(apiReturnedType.getType()) ? (T)Boolean.TRUE
              : Long.class.equals(apiReturnedType.getType()) ? (T)Long.valueOf(0L)
                : apiReturnedType.getType().getTypeName().startsWith(List.class.getName()) ? (T)List.of()
                : Void.class.equals(apiReturnedType.getType()) ? (T)voidMock
                : (T)Mockito.mock(Class.forName(apiReturnedType.getType().getTypeName()));

            Mockito.doReturn(ResponseEntity.ok(expectedResult))
              .when(restTemplateMock)
              .exchange(
                Mockito.argThat(req ->
                  req.getHeaders().getOrDefault(HttpHeaders.AUTHORIZATION, Collections.emptyList()).getFirst()
                    .equals("Bearer " + accessToken)),
                Mockito.eq(apiReturnedType));
            return Pair.of(accessToken, expectedResult);
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        })
        .toList();
    } else { // NO_AUTH
      try {
        T expectedResult =
          String.class.equals(apiReturnedType.getType()) ? (T)"RESULT"
          : Integer.class.equals(apiReturnedType.getType()) ? (T)Integer.valueOf(0)
          : Long.class.equals(apiReturnedType.getType()) ? (T)Long.valueOf(0L)
            : apiReturnedType.getType().getTypeName().startsWith(List.class.getName()) ? (T)List.of()
            : apiReturnedType.getType().getTypeName().startsWith(ResponseEntity.class.getName()) ? (T)ResponseEntity.ok()
            : Void.class.equals(apiReturnedType.getType()) ? (T)voidMock
            : (T)Mockito.mock(Class.forName(apiReturnedType.getType().getTypeName()));

        Mockito.doReturn(ResponseEntity.ok(expectedResult))
          .when(restTemplateMock)
          .exchange(
            Mockito.argThat(req ->
              !req.getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)),
            Mockito.eq(apiReturnedType));
        useCases = List.of(Pair.of(null, expectedResult));
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    try (ExecutorService executorService = Executors.newFixedThreadPool(10)) {
      executorService.invokeAll(useCases.stream()
        .map(p -> (Callable<?>) () -> {
          // Given
          String accessToken = p.getKey();
          T expectedResult = p.getValue();

          // When
          T result = apiInvoke.apply(accessToken);

          // Then
          Assertions.assertSame(expectedResult, result);
          return true;
        })
        .toList())
        .forEach(future -> {
          try {
            future.get();
          } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
          }
        });
    }

    apiUnloader.run();

    Mockito.verify(restTemplateMock, Mockito.times(useCases.size()))
      .exchange(Mockito.any(), Mockito.<ParameterizedTypeReference<?>>any());
  }
}
