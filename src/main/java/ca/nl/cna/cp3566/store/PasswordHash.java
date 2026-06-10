package ca.nl.cna.cp3566.store;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * INFRASTRUCTURE (given — do not change). You CALL this; you do not write crypto.
 *
 * Turns a password into something safe to store, using only the JDK. You never
 * store the password itself — you store the result of running it through PBKDF2
 * with a random per-user salt, many times. To check a login you run the typed
 * password through the same process and compare. If the data leaks, the attacker
 * gets hashes, not passwords.
 *
 * Stored string format:  iterations:base64Salt:base64Hash
 *
 *   PasswordHash.create(password)        -> the string to store
 *   PasswordHash.verify(password, stored) -> true if it matches
 */
public final class PasswordHash {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RNG = new SecureRandom();

    private PasswordHash() { }

    /** Hash a fresh password for storage. */
    public static String create(String password) {
        byte[] salt = new byte[16];
        RNG.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS);
        return ITERATIONS + ":" + b64(salt) + ":" + b64(hash);
    }

    /** Check a typed password against a stored "iterations:salt:hash" string. */
    public static boolean verify(String password, String stored) {
        try {
            String[] parts = stored.split(":");
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = unb64(parts[1]);
            byte[] expected = unb64(parts[2]);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations);
            return constantTimeEquals(expected, actual);
        } catch (RuntimeException e) {
            return false;  // malformed stored value -> treat as no match
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            KeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return f.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("PBKDF2 failed", e);
        }
    }

    /** Compare without leaking, via timing, where the first difference is. */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }

    private static String b64(byte[] b) { return Base64.getEncoder().encodeToString(b); }
    private static byte[] unb64(String s) { return Base64.getDecoder().decode(s); }
}
