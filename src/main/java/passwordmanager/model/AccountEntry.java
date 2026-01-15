package passwordmanager.model;

public class AccountEntry {
    private String platform;
    private String login;
    private String password;

    public AccountEntry(String platform, String login, String password) {
        this.platform = platform;
        this.login = login;
        this.password = password;
    }

    // GETTERS
    public String getPlatform() {
        return platform;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    // SETTERS
    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "AccountEntry{" +
                "platform='" + platform + '\'' +
                ", login='" + login + '\'' +
                ", password='" + password +
                '}';
    }
}