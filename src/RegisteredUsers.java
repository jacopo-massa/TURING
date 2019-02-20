import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.concurrent.ConcurrentHashMap;

public class RegisteredUsers extends RemoteServer implements IntRegistration
{
    private ConcurrentHashMap<String, UserInfo> users;
    public RegisteredUsers()
    {
        this.users = new ConcurrentHashMap<>();
    }

    public boolean registerUser(String username, String password) throws RemoteException, NullPointerException
    {
        if(username == null || password == null)
            throw new NullPointerException();

        if(users.containsKey(username))
            return false;

        UserInfo u = new UserInfo(password);
        users.putIfAbsent(username, u);
        return true;
    }

    public boolean unregisterUser(String username, String password) throws RemoteException, NullPointerException
    {
        if(!checkData(username,password))
            return false;

        users.remove(username);
        return true;
    }

    UserInfo getUser(String username, String password)
    {
        if(!checkData(username,password))
            return null;

        return users.get(username);
    }

    boolean setStatus(String username, String password, int online)
    {
        if(!checkData(username,password))
            return false;

        if(online==1)
            users.get(username).setOnline();
        else
            users.get(username).setOffline();

        return true;
    }

    /* controlla la consistenza e l'esistenza dell'username e della password */
    private boolean checkData(String username, String password)
    {
        return (username != null && password != null && users.containsKey(username));
    }
}
