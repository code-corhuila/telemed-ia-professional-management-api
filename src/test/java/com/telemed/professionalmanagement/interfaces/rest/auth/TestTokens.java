package com.telemed.professionalmanagement.interfaces.rest.auth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Builds JWTs for tests. Only tests hold a private key; the service never does. */
public final class TestTokens {

    public static final KeyPair TRUSTED = newKeyPair();
    public static final KeyPair OTHER = newKeyPair();

    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();

    private TestTokens() {
    }

    public static RSAPublicKey trustedPublicKey() {
        return (RSAPublicKey) TRUSTED.getPublic();
    }

    /** PEM in one line with literal \n, as a .env file stores it (Anexo C). */
    public static String trustedPublicKeyPemOneLine() {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(TRUSTED.getPublic().getEncoded());
        return ("-----BEGIN PUBLIC KEY-----\n" + body + "\n-----END PUBLIC KEY-----").replace("\n", "\\n");
    }

    public static String rs256(String payloadJson) {
        return signedWith(TRUSTED.getPrivate(), "{\"alg\":\"RS256\",\"typ\":\"JWT\"}", payloadJson);
    }

    public static String signedWith(PrivateKey key, String headerJson, String payloadJson) {
        String signingInput = encode(headerJson) + "." + encode(payloadJson);
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + B64URL.encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The classic attack: HS256 using the public key bytes as the shared secret. */
    public static String hs256WithPublicKeyAsSecret(String payloadJson) {
        String signingInput = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}") + "." + encode(payloadJson);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(TRUSTED.getPublic().getEncoded(), "HmacSHA256"));
            return signingInput + "." + B64URL.encodeToString(
                    mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String unsigned(String payloadJson) {
        return encode("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + encode(payloadJson) + ".";
    }

    public static String encode(String json) {
        return B64URL.encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /** {@code Authorization} value for a token shaped as the identity service issues it. */
    public static String bearer(String subject, String role) {
        long exp = java.time.Instant.now().getEpochSecond() + 3600;
        return "Bearer " + rs256("{\"iss\":\"telemed-ia-identity-and-access\",\"sub\":\"" + subject
                + "\",\"role\":\"" + role + "\",\"exp\":" + exp + "}");
    }

    private static KeyPair newKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
