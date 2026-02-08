# Password Manager (JavaFX)

A small password manager written in Java with a JavaFX UI.
It stores entries as platform / login / password, keeps the saved passwords encrypted in a local JSON file, and requires a master password to open the app.

This is a learning project focused on combining a clean GUI flow with local persistence and basic cryptography.

## What it can do

- Master password setup on first run (with strength requirements)
- Login throttling after multiple failed attempts (increasing wait times)
- Add, edit, delete saved entries
- Password field show/hide
- Generate password button
- Double click on an entry to open a details dialog with:
    - show/hide password
    - copy password to clipboard
- Local storage:
    - passwords.json (entries with encrypted passwords)
    - master.hash (SHA-256 hash of the master password, Base64)
- Export:
    - Plaintext CSV (decrypted passwords) to password_export.csv
    - Encrypted JSON export to password_export_encrypted.json

## Project structure

```
project-root/
├── README.md
├── gson.jar
├── src/
│   └── main/java/passwordmanager/
│                   ├── logic/
│                   │   ├── CryptoUtils.java
│                   │   ├── MasterPasswordManager.java
│                   │   └── PasswordManager.java
│                   ├── model/
│                   │   └── AccountEntry.java
│                   └── ui/
│                       └── Main.java
```

## How it works

### Master password

On first run there is no master.hash, so the app asks you to set the master password.

Strength requirements:
- at least 8 characters
- one uppercase letter
- one lowercase letter
- one digit
- one special character

The app stores SHA-256(masterPassword) encoded as Base64 in master.hash.
On login it hashes the input again and compares the result.

### Stored entries

Entries are stored in passwords.json.
The password field in each entry is encrypted using AES and then Base64-encoded.

### Exports

- Plaintext CSV export writes decrypted passwords to password_export.csv
- Encrypted JSON export writes the current encrypted list to password_export_encrypted.json

## Files created by the app

All files are created in the working directory (the folder you start the app from):

- `master.hash`: SHA-256 hash of the master password (Base64)
- `passwords.json`: local database (passwords are encrypted)
- `password_export.csv`: plaintext export (only if you export)
- `password_export_encrypted.json`: encrypted export (only if you export)

### Reset and clean start

To reset the application state, delete `master.hash` and `passwords.json`.

## Requirements

- Java 17+ (Java 11 might work, but tested on a newer JDK)
- JavaFX (controls)
- Gson (JSON serialization)

Linux packages example (Debian/Ubuntu):

```bash
sudo apt update
sudo apt install openjdk-17-jdk openjfx
```

Gson can be added as a single JAR file to the project (gson.jar).

## Running the project (no Maven or Gradle)

This project is run directly (no build tool). You need:
- JDK (javac and java)
- JavaFX SDK or OpenJFX installed on your system
- Gson JAR on the classpath

### Option 1: Run from an IDE (recommended)
In IntelliJ IDEA:
- Set Project SDK to your JDK (for example JDK 17)
- Add JavaFX to the project (JavaFX SDK or system OpenJFX)
- Add Gson as a library (the gson JAR)
- Run the main class: passwordmanager.ui.Main

### Option 2: Run from the command line (Linux/macOS, bash)

1) Prepare paths

Set the path to your JavaFX libraries (the folder that contains javafx.controls.jar etc.).
On some systems it can be /usr/share/openjfx/lib, but it depends on how JavaFX is installed.

Also point to your Gson JAR file (its name may look like gson-2.x.x.jar).

Example:
```bash
JAVAFX_LIB="/usr/share/openjfx/lib"
GSON_JAR="gson-2.x.x.jar"

```
2) Compile (Linux with OpenJFX installed from packages)

```bash
mkdir -p out

javac \
  --module-path "$JAVAFX_LIB" \
  --add-modules javafx.controls \
  -cp "$GSON_JAR" \
  -d out \
  $(find src/main/java -name "*.java")

```

3) Run

```bash
java \
  --module-path /usr/share/openjfx/lib \
  --add-modules javafx.controls \
  -cp "out:gson.jar" \
  passwordmanager.ui.Main
```

On Windows, replace `out:gson.jar` with `out;gson.jar`.

## Security notes

Passwords are stored encrypted and the master password is never saved in plaintext.
To increase real-world security, the project could be extended with stronger key derivation (e.g. PBKDF2 or Argon2 with salt), authenticated encryption modes (AES-GCM), and safe file storage in a protected directory.