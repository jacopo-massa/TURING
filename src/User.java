public class User
{
    private String username;
    private String password;
    private int online;

    public User(String username, String password)
    {
        this.username = username;
        this.password = password;
        this.online = 1;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isOnline()
    {
        return (online == 1);
    }

    public void setOnline() {
        this.online = 1;
    }

    public void setOffline() {
        this.online = 0;
    }
}
