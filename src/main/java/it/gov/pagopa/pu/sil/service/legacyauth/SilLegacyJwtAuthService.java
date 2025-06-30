package it.gov.pagopa.pu.sil.service.legacyauth;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import it.gov.pagopa.pu.auth.dto.generated.AccessToken;
import it.gov.pagopa.pu.organization.dto.generated.JwtAlgorithm;
import it.gov.pagopa.pu.organization.dto.generated.SilServiceLegacyJwtAuthConfig;
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

  public SilLegacyJwtAuthService(
    @Value("${legacy-auth.jwt-legacy-expiration-seconds}") Integer expirationTimeInSeconds) {
    this.expirationTimeInSeconds = expirationTimeInSeconds;
  }

  public AccessToken authenticate(SilServiceLegacyJwtAuthConfig config) {
    // TODO: fix config.signingKey value! actually you are reading a byte[] because it's a ciphered String, it should expected to have a Base64 String instead!
    Algorithm algorithm = resolveAlgorithm(config.getAlgorithm(), config.getSigningKey());

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

  private Algorithm resolveAlgorithm(JwtAlgorithm algorithm, byte[] signingKey) {
    if (algorithm == null) {
      throw new IllegalArgumentException("Algorithm must not be null");
    }
    switch (algorithm) {
      case JwtAlgorithm.HS256 -> Algorithm.HMAC256(signingKey);
      case JwtAlgorithm.HS384 -> Algorithm.HMAC384(signingKey);
      case JwtAlgorithm.HS512 -> Algorithm.HMAC512(signingKey);
      case JwtAlgorithm.RS256, JwtAlgorithm.RS384, JwtAlgorithm.RS512 ->
        throw new UnsupportedOperationException("RSA algorithms require a public/private key pair. Not implemented in this method.");
      case JwtAlgorithm.ES256, JwtAlgorithm.ES384, JwtAlgorithm.ES512 ->
        throw new UnsupportedOperationException("EC algorithms require a public/private key pair. Not implemented in this method.");
    }
    return null;
  }
}
