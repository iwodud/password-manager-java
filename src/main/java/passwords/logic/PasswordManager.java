package passwords.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import passwords.model.AccountEntry;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PasswordManager {
    private List<AccountEntry> entries = new ArrayList<>();
    private final String FILE_NAME = "passwords.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void addEntry(AccountEntry entry) {
        entries.add(entry);
    }

    public List<AccountEntry> getAllEntries() {
        return entries;
    }

    public void saveToFile() {
        try (FileWriter writer = new FileWriter(FILE_NAME)) {
            gson.toJson(entries, writer);
            System.out.println("Data successfully saved to " + FILE_NAME);
        } catch (IOException e) {
            System.err.println("Error while saving: " + e.getMessage());
        }
    }
}