package it.gov.pagopa.pu.sil.service.legacyauth;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.JwtAlgorithm;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyJwtAuthConfig;
import it.gov.pagopa.pu.sil.service.AlgorithmResolverService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SilLegacyJwtAuthService {
  public static final String ACCESS_TOKEN_TYPE = "at+JWT";

  private final Integer expirationTimeInSeconds;
  private final AlgorithmResolverService algorithmResolverService;
  private final Map<JwtAlgorithm, Pair<Instant, Algorithm>> algorithmCachedMap = new ConcurrentHashMap<>();

  public SilLegacyJwtAuthService(
    @Value("${legacy-auth.jwt-legacy-expiration-seconds}") Integer expirationTimeInSeconds,
    AlgorithmResolverService algorithmResolverService) {
      this.expirationTimeInSeconds = expirationTimeInSeconds;
      this.algorithmResolverService = algorithmResolverService;
  }

  public AccessToken authenticate(SilServiceLegacyJwtAuthConfig config) {
    // TODO: fix config.signingKey value! actually you are reading a byte[] because it's a ciphered String, it should expected to have a Base64 String instead!
    String encodedToString = Base64.getEncoder().encodeToString(config.getSigningKey());
    JwtAlgorithm jwtAlgorithm = config.getAlgorithm();
    Algorithm algorithm = algorithmCachedMap.compute(jwtAlgorithm, (k, v) -> {
      Instant now = Instant.now();
      if (v == null || now.isAfter(v.getLeft())) {
        Algorithm resolved = algorithmResolverService.resolveAlgorithm(jwtAlgorithm, encodedToString);
        Instant expiration = now.plusSeconds(expirationTimeInSeconds - 5L);
        return Pair.of(expiration, resolved);
      } else {
        return v;
      }
    }).getRight();

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
