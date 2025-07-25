package it.gov.pagopa.pu.sil.config.agid;

import com.nimbusds.jose.crypto.RSASSASigner;
import it.gov.pagopa.pu.sil.util.AgidUtils;
import it.gov.pagopa.pu.sil.util.CertUtils;
import jakarta.annotation.Nonnull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

/**
 * It will intercept and configure PDND auth metadata
 */
public class AgidDataIntegrityInterceptor implements ClientHttpRequestInterceptor {

  private final PuIntegrityDataConfig puIntegrityDataConfig;
  private RSASSASigner jwsRsaSigner = null;

  public AgidDataIntegrityInterceptor(PuIntegrityDataConfig puIntegrityDataConfig) {
    this.puIntegrityDataConfig = puIntegrityDataConfig;
  }

  // This method lazily builds the JWS signer using the private key from the configuration.
  private RSASSASigner getJwsRsaSigner() {
    if(this.jwsRsaSigner == null) {
      this.jwsRsaSigner = buildJwsSigner(puIntegrityDataConfig);
    }
    return this.jwsRsaSigner;
  }

  @Override
  @Nonnull
  public ClientHttpResponse intercept(@Nonnull HttpRequest request, @Nonnull byte[] body, @Nonnull ClientHttpRequestExecution execution) throws IOException {
    String digest = AgidUtils.buildDigest(new String(body, StandardCharsets.UTF_8));

    HttpHeaders headers = request.getHeaders();
    headers.add(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name());
    headers.add("Agid-JWT-Signature", AgidUtils.buildAgidJwtSignature(digest, puIntegrityDataConfig, getJwsRsaSigner()));
    headers.add("Digest", digest);

    return execution.execute(request, body);
  }

  private RSASSASigner buildJwsSigner(PuIntegrityDataConfig puIntegrityDataConfig) {
    try {
      return new RSASSASigner(CertUtils.<PrivateKey>pemKey2PrivateKey("RSA", puIntegrityDataConfig.getPrivateKey()));
    } catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e) {
      throw new IllegalStateException("Cannot build JWS Signer for PU service having clientId:" + puIntegrityDataConfig.getClientId(), e);
    }
  }
}
