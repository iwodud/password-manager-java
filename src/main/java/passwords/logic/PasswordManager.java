package passwords.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import passwords.model.AccountEntry;

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
            System.out.println("Data successfully saved to " + FILE_NAME);
        } catch (IOException e) {
            System.err.println("Error while saving: " + e.getMessage());
        }
    }

    public void loadFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            // Define the type of data we want to load (a list of AccountEntry objects)
            Type listType = new TypeToken<ArrayList<AccountEntry>>(){}.getType();
            entries = gson.fromJson(reader, listType);

            if (entries == null) {
                entries = new ArrayList<>();
            }
            System.out.println("Data successfully loaded from " + FILE_NAME);
        } catch (IOException e) {
            System.err.println("Error while loading: " + e.getMessage());
        }
    }
}