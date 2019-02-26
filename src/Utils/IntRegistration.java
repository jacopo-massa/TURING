package Utils;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IntRegistration extends Remote
{
    boolean registerUser(String username, String password) throws RemoteException, NullPointerException;
}

