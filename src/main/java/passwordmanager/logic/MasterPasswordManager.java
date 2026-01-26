package passwordmanager.logic;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;

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

    public boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;

        Pattern upper = Pattern.compile("[A-Z]");
        Pattern lower = Pattern.compile("[a-z]");
        Pattern digit = Pattern.compile("[0-9]");
        Pattern special = Pattern.compile("[^A-Za-z0-9]");

        return upper.matcher(password).find()
                && lower.matcher(password).find()
                && digit.matcher(password).find()
                && special.matcher(password).find();
    }
}
