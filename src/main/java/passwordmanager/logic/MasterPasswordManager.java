package passwordmanager.logic;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class MasterPasswordManager {

    private static final String MASTER_FILE = "master.hash";

    public boolean isPasswordSet() {
        return new File(MASTER_FILE).exists();
    }

    public void setPassword(String password) {
        String hash = hashPassword(password);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(MASTER_FILE))) {
            writer.write(hash);
        } catch (IOException e) {
            System.err.println("Master password entry error.");
        }
    }

    public boolean verifyPassword(String inputPassword) {
        File file = new File(MASTER_FILE);
        if (!file.exists()) return false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String savedHash = reader.readLine();
            String inputHash = hashPassword(inputPassword);
            return savedHash.equals(inputHash);
        } catch (IOException e) {
            return false;
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available");
        }
    }
}
