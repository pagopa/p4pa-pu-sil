package it.gov.pagopa.pu.sil.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nimbusds.jose.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.util.Date;

public class JWTUtils {
  private JWTUtils() {
  }

  public static SignedJWT signJwt(JWTClaimsSet claims, String kid, JWSAlgorithm algorithm, JWSSigner jwsSigner){
      try {
          SignedJWT signedJWT = new SignedJWT(
                  new JWSHeader.Builder(algorithm)
                          .type(JOSEObjectType.JWT)
                          .keyID(kid)
                          .build(),
                  claims
          );
          signedJWT.sign(jwsSigner);
          return signedJWT;
      } catch (JOSEException e) {
          throw new IllegalStateException("Error building PDND client assertion", e);
      }
  }

  public static boolean isJWTExpired(String token) {
    try {
        DecodedJWT decodedJWT = JWT.decode(token);
        Date expiresAt = decodedJWT.getExpiresAt();
        return expiresAt.before(new Date());
    } catch (JWTDecodeException e) {
        throw new JWTDecodeException(e.getMessage());
    }
  }
}
