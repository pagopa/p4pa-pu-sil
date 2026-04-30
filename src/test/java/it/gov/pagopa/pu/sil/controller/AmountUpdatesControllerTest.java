package it.gov.pagopa.pu.sil.controller;

import it.gov.pagopa.pu.auth.dto.generated.UserInfo;
import it.gov.pagopa.pu.sil.dto.generated.ActualizationResultDTO;
import it.gov.pagopa.pu.sil.service.actualization.ActualizationFacadeService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AmountUpdatesControllerTest {
  @Mock
  private ActualizationFacadeService actualizationFacadeServiceMock;

  @InjectMocks
  private AmountUpdatesController amountUpdatesController;

  private UserInfo userInfo;

  @BeforeEach
  void setUp() {
    userInfo = new UserInfo();
    userInfo.setMappedExternalUserId("fakeExternalUser");
    Authentication authentication = new UsernamePasswordAuthenticationToken(userInfo, "fakeAccessToken");
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  void whenGetAmountUpdatesThenOk() {
    Long orgSilServiceId = 1L;
    String nav = "NAV123";
    ActualizationResultDTO expectedResult = new ActualizationResultDTO();

    Mockito.when(actualizationFacadeServiceMock.actualize(orgSilServiceId, nav, userInfo, "fakeAccessToken"))
            .thenReturn(expectedResult);

    ResponseEntity<ActualizationResultDTO> response = amountUpdatesController.actualize(orgSilServiceId, nav);

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertSame(expectedResult, response.getBody());
  }

}
