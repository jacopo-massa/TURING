public class UserInfo
{
    private String password;
    private int online;

    public UserInfo(String password)
    {
        this.password = password;
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
