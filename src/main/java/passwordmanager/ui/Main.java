package passwordmanager.ui;

import passwordmanager.logic.PasswordManager;
import passwordmanager.logic.MasterPasswordManager;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        MasterPasswordManager master = new MasterPasswordManager();

        if (!master.isPasswordSet()) {
            System.out.print("Set a master password: ");
            String newPassword = scanner.nextLine();
            master.setPassword(newPassword);
            System.out.println("Master password has been saved.");
        }

        System.out.print("Enter master password: ");
        String input = scanner.nextLine();

        if (!master.verifyPassword(input)) {
            System.out.println("Incorrect password. Exiting.");
            return;
        }

        System.out.println("Successfully logged in!");

        PasswordManager manager = new PasswordManager();
        manager.loadFromFile();
    }
}
