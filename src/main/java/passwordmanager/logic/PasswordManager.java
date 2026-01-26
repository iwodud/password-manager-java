package passwordmanager.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import passwordmanager.model.AccountEntry;

import java.io.*;
import java.lang.reflect.Type;
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
            System.out.println("Saved to " + FILE_NAME);
        } catch (IOException e) {
            System.err.println("Save error: " + e.getMessage());
        }
    }

    public void loadFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<AccountEntry>>(){}.getType();
            List<AccountEntry> loadedEntries = gson.fromJson(reader, listType);
            if (loadedEntries != null) {
                this.entries = loadedEntries;
            }
            System.out.println("Loaded from " + FILE_NAME);
        } catch (IOException e) {
            System.err.println("Load error: " + e.getMessage());
        }
    }

    public void removeEntry(AccountEntry entry) {
        entries.remove(entry);
    }

    public void saveToFile(Writer writer) {
        try {
            gson.toJson(entries, writer);
            writer.flush();
        } catch (IOException e) {
            System.err.println("Export to writer failed: " + e.getMessage());
        }
    }

}