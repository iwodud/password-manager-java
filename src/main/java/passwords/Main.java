package passwords;

import passwords.logic.PasswordManager;
import passwords.model.AccountEntry;

public class Main {
    public static void main(String[] args) {
        PasswordManager manager = new PasswordManager();

        manager.loadFromFile();

        System.out.println("Initial entries: " + manager.getAllEntries().size());

        manager.addEntry(new AccountEntry("Google", "mymail@gmail.com", "GooglePassword123!"));
        manager.addEntry(new AccountEntry("Facebook", "FBUser", "FacebookPassword123"));

        manager.saveToFile();

        System.out.println("Final entries: " + manager.getAllEntries());
    }
}