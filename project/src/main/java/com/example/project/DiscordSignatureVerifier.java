package com.example.project;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

@Component
public class DiscordSignatureVerifier {

    @Value("${discord.public.key}")
    private String publicKeyHex;

    public boolean verify(String body, String signatureHex, String timestamp) {

        try {
            byte[] publicKeyBytes = hexToBytes(publicKeyHex);

            // Discord signs: timestamp + raw request body
            String message = timestamp + body;

            byte[] messageBytes =
                    message.getBytes(StandardCharsets.UTF_8);

            byte[] signatureBytes =
                    hexToBytes(signatureHex);

            // Ed25519 public key in X.509 SubjectPublicKeyInfo format
            byte[] x509Prefix = hexToBytes(
                    "302a300506032b6570032100"
            );

            byte[] encodedPublicKey =
                    new byte[x509Prefix.length + publicKeyBytes.length];

            System.arraycopy(
                    x509Prefix, 0,
                    encodedPublicKey, 0,
                    x509Prefix.length
            );

            System.arraycopy(
                    publicKeyBytes, 0,
                    encodedPublicKey, x509Prefix.length,
                    publicKeyBytes.length
            );

            KeyFactory keyFactory =
                    KeyFactory.getInstance("Ed25519");

            PublicKey publicKey =
                    keyFactory.generatePublic(
                            new X509EncodedKeySpec(encodedPublicKey)
                    );

            Signature verifier =
                    Signature.getInstance("Ed25519");

            verifier.initVerify(publicKey);
            verifier.update(messageBytes);

            return verifier.verify(signatureBytes);

        } catch (Exception e) {
            return false;
        }
    }

    private byte[] hexToBytes(String hex) {

        int length = hex.length();
        byte[] result = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            result[i / 2] =
                    (byte) Integer.parseInt(
                            hex.substring(i, i + 2),
                            16
                    );
        }

        return result;
    }
}