package com.loopin.api.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private static final URI GOOGLE_CERTS_URI = URI.create("https://www.googleapis.com/oauth2/v1/certs");
    private static final Pattern JSON_STRING_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern JSON_BOOLEAN_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*(true|false)");
    private static final Pattern JSON_NUMBER_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*(\\d+)");
    private static final Pattern CERT_ENTRY_PATTERN = Pattern.compile(
            "\"([^\"]+)\"\\s*:\\s*\"(-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----)\"",
            Pattern.DOTALL
    );

    private final HttpClient httpClient;
    private final String googleClientId;

    private Map<String, X509Certificate> cachedCertificates = Map.of();
    private Instant certificatesExpireAt = Instant.EPOCH;

    public GoogleTokenVerifierImpl(@Value("${google.client-id}") String googleClientId) {
        this.httpClient = HttpClient.newHttpClient();
        this.googleClientId = googleClientId;
    }

    @Override
    public GoogleTokenClaims verify(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw unauthorized("Invalid Google ID token");
            }

            String headerJson = decodeBase64Url(parts[0]);
            String payloadJson = decodeBase64Url(parts[1]);

            String algorithm = requiredString(headerJson, "alg");
            if (!"RS256".equals(algorithm)) {
                throw unauthorized("Unsupported Google ID token algorithm");
            }

            String keyId = requiredString(headerJson, "kid");
            X509Certificate certificate = getCertificates().get(keyId);
            if (certificate == null) {
                refreshCertificates();
                certificate = getCertificates().get(keyId);
            }
            if (certificate == null || !isSignatureValid(parts, certificate)) {
                throw unauthorized("Invalid Google ID token signature");
            }

            validateClaims(payloadJson);

            return new GoogleTokenClaims(
                    requiredString(payloadJson, "sub"),
                    requiredString(payloadJson, "email"),
                    optionalString(payloadJson, "name", requiredString(payloadJson, "email"))
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unauthorized("Invalid Google ID token");
        }
    }

    private void validateClaims(String payloadJson) {
        String issuer = requiredString(payloadJson, "iss");
        if (!"https://accounts.google.com".equals(issuer) && !"accounts.google.com".equals(issuer)) {
            throw unauthorized("Invalid Google ID token issuer");
        }

        String audience = requiredString(payloadJson, "aud");
        if (!googleClientId.equals(audience)) {
            throw unauthorized("Invalid Google ID token audience");
        }

        long expiresAt = requiredLong(payloadJson, "exp");
        if (Instant.ofEpochSecond(expiresAt).isBefore(Instant.now())) {
            throw unauthorized("Expired Google ID token");
        }

        if (!requiredBoolean(payloadJson, "email_verified")) {
            throw unauthorized("Google account email is not verified");
        }
    }

    private synchronized Map<String, X509Certificate> getCertificates() throws Exception {
        if (Instant.now().isAfter(certificatesExpireAt)) {
            refreshCertificates();
        }
        return cachedCertificates;
    }

    private synchronized void refreshCertificates() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(GOOGLE_CERTS_URI).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw unauthorized("Unable to load Google certificates");
        }

        Map<String, X509Certificate> certificates = new LinkedHashMap<>();
        Matcher matcher = CERT_ENTRY_PATTERN.matcher(response.body());
        while (matcher.find()) {
            certificates.put(matcher.group(1), parseCertificate(matcher.group(2)));
        }

        if (certificates.isEmpty()) {
            throw unauthorized("Unable to load Google certificates");
        }

        cachedCertificates = certificates;
        certificatesExpireAt = Instant.now().plusSeconds(3600);
    }

    private X509Certificate parseCertificate(String certificatePem) throws Exception {
        String normalized = certificatePem
                .replace("\\n", "\n")
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(normalized);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(der));
    }

    private boolean isSignatureValid(String[] tokenParts, X509Certificate certificate) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(certificate.getPublicKey());
        signature.update((tokenParts[0] + "." + tokenParts[1]).getBytes(StandardCharsets.US_ASCII));
        return signature.verify(Base64.getUrlDecoder().decode(tokenParts[2]));
    }

    private String decodeBase64Url(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private String requiredString(String json, String field) {
        return optionalString(json, field, null);
    }

    private String optionalString(String json, String field, String defaultValue) {
        Matcher matcher = Pattern.compile(String.format(JSON_STRING_PATTERN.pattern(), Pattern.quote(field))).matcher(json);
        if (!matcher.find()) {
            if (defaultValue != null) {
                return defaultValue;
            }
            throw unauthorized("Missing Google ID token claim: " + field);
        }
        return matcher.group(1);
    }

    private long requiredLong(String json, String field) {
        Matcher matcher = Pattern.compile(String.format(JSON_NUMBER_PATTERN.pattern(), Pattern.quote(field))).matcher(json);
        if (!matcher.find()) {
            throw unauthorized("Missing Google ID token claim: " + field);
        }
        return Long.parseLong(matcher.group(1));
    }

    private boolean requiredBoolean(String json, String field) {
        Matcher matcher = Pattern.compile(String.format(JSON_BOOLEAN_PATTERN.pattern(), Pattern.quote(field))).matcher(json);
        if (!matcher.find()) {
            throw unauthorized("Missing Google ID token claim: " + field);
        }
        return Boolean.parseBoolean(matcher.group(1));
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
