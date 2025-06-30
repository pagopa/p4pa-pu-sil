package it.gov.pagopa.pu.sil.service.legacyauth;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyJwtAuthConfig;
import it.gov.pagopa.pu.sil.service.AlgorithmResolverService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SilLegacyJwtAuthService {
  public static final String ACCESS_TOKEN_TYPE = "at+JWT";

  private final Integer expirationTimeInSeconds;
  private final AlgorithmResolverService algorithmResolverService;

  public SilLegacyJwtAuthService(
    @Value("${legacy-auth.jwt-legacy-expiration-seconds}") Integer expirationTimeInSeconds,
    AlgorithmResolverService algorithmResolverService) {
      this.expirationTimeInSeconds = expirationTimeInSeconds;
      this.algorithmResolverService = algorithmResolverService;
  }

  public AccessToken authenticate(SilServiceLegacyJwtAuthConfig config) {
    // TODO: fix config.signingKey value! actually you are reading a byte[] because it's a ciphered String, it should expected to have a Base64 String instead!
    Algorithm algorithm = algorithmResolverService.resolveAlgorithm(config.getAlgorithm(), config.getSigningKey());

    Map<String, Object> headerClaims = new HashMap<>();
    headerClaims.put(HeaderParams.KEY_ID, config.getKid());
    headerClaims.put("typ", ACCESS_TOKEN_TYPE);
    headerClaims.put("alg", config.getAlgorithm());
    String tokenType = "bearer";
    JWTCreator.Builder jwtBuilder = JWT.create()
      .withHeader(headerClaims)
      .withClaim("typ", tokenType)
      .withIssuer(config.getIssuer())
      .withJWTId(UUID.randomUUID().toString())
      .withSubject(config.getSubject())
      .withIssuedAt(Instant.now())
      .withExpiresAt(Instant.now().plusSeconds(expirationTimeInSeconds));
    String token = jwtBuilder
      .sign(algorithm);
    return new AccessToken(token, tokenType, expirationTimeInSeconds);
  }
}
