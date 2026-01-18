package passwordmanager.logic;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CryptoUtils {

    private static final String ALGORITHM = "AES";

    private static SecretKeySpec getKey(String masterPassword) {
        byte[] keyBytes = new byte[16];
        byte[] passwordBytes = masterPassword.getBytes();

        for (int i = 0; i < keyBytes.length && i < passwordBytes.length; i++) {
            keyBytes[i] = passwordBytes[i];
        }

        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public static String encrypt(String data, String masterPassword) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(masterPassword));
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption error");
        }
    }

    public static String decrypt(String encryptedData, String masterPassword) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getKey(masterPassword));
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Decryption error");
        }
    }
}
