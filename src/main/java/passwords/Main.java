package passwords;

import passwords.logic.PasswordManager;
import passwords.model.AccountEntry;

public class Main {
    public static void main(String[] args) {        // 1. Tworzymy menedżera
        PasswordManager manager = new PasswordManager();

        manager.addEntry(new AccountEntry("Google", "mymail@gmail.com", "GooglePassword123!"));
        manager.addEntry(new AccountEntry("Facebook", "FBUser", "FacebookPassword123"));

        manager.saveToFile();

        System.out.println("Loading finished. Check passwords.json file in directory of the projeckt.");
    }
}