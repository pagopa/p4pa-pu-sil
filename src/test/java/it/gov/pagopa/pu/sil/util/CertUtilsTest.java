package it.gov.pagopa.pu.sil.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.*;

public class CertUtilsTest {

    public static final String PRIVATE_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCNK9IOasciiHSy
            +pKOelylc9gnjWfIO/x74XFmjixeJr2xZQlA4nKkNv0u+CO40XgSH1OfiNxLZPJd
            L77Ei73462FVabSUmK3Lv6DJlNe+51a6zOx8KqpyM+EirAcZ6OQPQi1Q0T/XFrho
            jEAV4GckPuvzppbxp7RBQFfN9WYvzghYKqbuZ6I+OS2RoetOtASXamzRdZT3gK/0
            X0VOZo1JSDp9vcSFa+dlddgc+Ns3rso5tE9UNqplmTyBK7AehUuBNU6d8rQbvmgw
            ORY2d47UZe+XsfXQn2UfYi5movxzs0cwqEdXmqbOIESbpT3y2jBWDGUHNn7faIhi
            WqKdLS3ZAgMBAAECgf8NAg0nkG/dQUYXolBBrS/SHkO8JvkZUG21V8XiN3HSen18
            ZTwlNEj5J0glfJwFzUJRC2RMsJBcnvRssPhOy1I7Br7jU+MKJGUy8EFvyNFsEyHV
            Mwo8LvvFDfiYsa5G8HmFTrsJULhat9A+nLfoJh9LsBsxRU0uvPLgXus0goBRLjBz
            0Y4iUJgLjjKDGGIMtNrfrAdPcaQ/vYHT+XNkbR3nXLqoeI+jzBMckHD6KG+Mq1NR
            uTOJJDzDNcq9YYgt/oTkvHubtG9s7Y9YdEK3UPsx7aG41YyW1Fq7J5pqv43c0PXs
            CJFGIv939FpRZHKh9WCT7b/LVacYBI68JuUflCMCgYEAw0MgIaNf/XC2zXjVya9M
            EzZHFpkUa7IKoNHkA7HQ8Ev77nYYlTnR7krAKBf332MNAO7ApFfO8T1b1mvwS0eQ
            hTpa/+7k6EzNOe8BZwyFht4iLknVH1Y+peHzQtdNBnuPdDhuGZlBFBMoQgCKAEuY
            ZW+34wDtsIHYC+RQPns/CXMCgYEAuRViww3MAZn6PMPMkkUXGeCoStx8cCL2XmPn
            Y1jUNi2OR55t4woJfSFQpcrRF7gXx85eriMSxoBGNZNh0Dj9M3uUTWqqr17MRLLa
            t1z0gpm2MHFC7WQOMVbajqS3//7B+bLTJAOI2vnp4oe8s7z1Sbno8mpFWiEZj8bE
            bWTxSIMCgYEAs+pL1vLUZY/PwC+QvU86R5GBmv7d5AWe6WO8NvNG08MPlT9Xk1g9
            aNWTjN3Y1QpNVwimlEccNQgWcNHwDU0ZisikRKH4ZVsu1iy1HCBbgFN5JzF8oG01
            OF+jZ3k+TbTYD3xXZlrhrf+g3n/kqDT/bKetxgp6+GILkZmDnq6s/KUCgYBemWCu
            Y6nnE6WEU2uHQ4sILfy2rrVnt2cHXbbR34Av5N75Gi/+QI4TB+kppF106yI0fPWF
            ueWJ0dyQ27C99bLtEnf9jcyJ8EElx+jkmb1b12b4oZtcrKxYaZUyHVzymmrYzp7+
            pFPZ4Ky7nTdFAwq4US6QYOLrq0leZHDXnSV6MwKBgQCLCiRgOh62DsrNQaOsxo9l
            G20FQqfwBqflmUqCtQNuE00JReN9Rh6i3PX1eBguIkUNFD149hkr8316KTxiyof3
            qh961xrZWqmnkV9XdAq9zA23IUlvFtyHkrBT/+AtEzjvzaj1LQisomlfIzLlBMBc
            3cln+ZXrEwAmFOkkYHlFag==
            -----END PRIVATE KEY-----
            """;

    public static final String PUBLIC_KEY = """
            -----BEGIN PUBLIC KEY-----
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjSvSDmrHIoh0svqSjnpc
            pXPYJ41nyDv8e+FxZo4sXia9sWUJQOJypDb9LvgjuNF4Eh9Tn4jcS2TyXS++xIu9
            +OthVWm0lJity7+gyZTXvudWuszsfCqqcjPhIqwHGejkD0ItUNE/1xa4aIxAFeBn
            JD7r86aW8ae0QUBXzfVmL84IWCqm7meiPjktkaHrTrQEl2ps0XWU94Cv9F9FTmaN
            SUg6fb3EhWvnZXXYHPjbN67KObRPVDaqZZk8gSuwHoVLgTVOnfK0G75oMDkWNneO
            1GXvl7H10J9lH2IuZqL8c7NHMKhHV5qmziBEm6U98towVgxlBzZ+32iIYlqinS0t
            2QIDAQAB
            -----END PUBLIC KEY-----
            """;

    @Test
    void givenValidPrivateKeyWhenPemKey2PrivateKeyThenValidKey() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        // When
        PrivateKey privateKey = CertUtils.pemKey2PrivateKey("RSA", PRIVATE_KEY);

        // Then
        assertNotNull(privateKey);
        assertInstanceOf(RSAPrivateKey.class, privateKey);
    }

    @Test
    void givenInvalidPrivateKeyWhenPemKey2PrivateKeyThenInvalidKey() {
        // Given
        String invalidPemKey = """
                -----BEGIN PRIVATE KEY-----
                NOT VALID KEY
                -----END PRIVATE KEY-----
                """;

        // Then
        assertThrows(InvalidKeySpecException.class, () -> CertUtils.pemKey2PrivateKey("RSA", invalidPemKey));
    }

    @Test
    void givenNullPrivateKeyWhenPemKey2PrivateKeyThenNullKey() {
        // Given
        String nullKey = null;

        // Then
        assertThrows(NullPointerException.class, () -> CertUtils.pemKey2PrivateKey("RSA", nullKey));
    }

    @Test
    void givenValidPemWhenExtractInlinePemBodyThenValidPem() {
        // Given
        String pemKey = """
                -----BEGIN PRIVATE KEY-----
                MIIEvQIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBALzbdGZIkI5wsRwl
                OjiZlQCvdS8/JXbbE29AQSkCAwEAAQ==
                -----END PRIVATE KEY-----
                """;

        // When
        String extractedBody = CertUtils.extractInlinePemBody(pemKey);

        // Then
        assertFalse(extractedBody.contains("BEGIN PRIVATE KEY"));
        assertFalse(extractedBody.contains("END PRIVATE KEY"));
        assertFalse(extractedBody.contains("\n"));
    }
}
