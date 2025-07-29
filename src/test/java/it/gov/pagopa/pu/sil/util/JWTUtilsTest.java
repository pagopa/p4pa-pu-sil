package it.gov.pagopa.pu.sil.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JWTUtilsTest {

//region test isJWTExpired
    @Test
    void givenValidTokenWhenIsJWTExpiredThenTokenNotExpired() {
        // Given
        Date futureDate = new Date(System.currentTimeMillis() + 3600 * 1000); // 1 hour from now
        String token = JWT.create()
                .withExpiresAt(futureDate)
                .sign(Algorithm.HMAC256("secret"));

        // Then
        assertFalse(JWTUtils.isJWTExpired(token));
    }

    @Test
    void givenExpiredTokenWhenIsJWTExpiredThenTokenExpired() {
        // Given
        Date pastDate = new Date(System.currentTimeMillis() - 3600 * 1000); // 1 hour ago
        String token = JWT.create()
                .withExpiresAt(pastDate)
                .sign(Algorithm.HMAC256("secret"));
        // Then
        assertTrue(JWTUtils.isJWTExpired(token));
    }

    @Test
    void givenInvalidTokenWhenIsJWTExpiredThenException() {
        // Given
        String invalidtoken = "INVALIDTOKEN";
        // Then
        assertThrows(JWTDecodeException.class, () -> JWTUtils.isJWTExpired(invalidtoken));
    }
//endregion

//region test signJwt
    @Test
    void whensignJwtThenOk() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        // Given
        RSASSASigner signer = new RSASSASigner(CertUtils.<RSAPrivateKey>pemKey2PrivateKey("RSA",CertUtilsTest.PRIVATE_KEY));
        JWTVerifier verifier = JWT.require(Algorithm.RSA512(CertUtils.<RSAPublicKey>pemPub2PublicKey("RSA", CertUtilsTest.PUBLIC_KEY))).build();

        // Then
        SignedJWT result = JWTUtils.signJwt(new JWTClaimsSet.Builder().build(), "KID", JWSAlgorithm.RS512, signer);

        // When
        Assertions.assertDoesNotThrow(() -> verifier.verify(result.serialize()));
    }
//endregion
}
