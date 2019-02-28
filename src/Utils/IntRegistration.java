package Utils;

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * Interfaccia per getire il servizio RMI di registrazione utenti a TURING
 */
public interface IntRegistration extends Remote
{
    boolean registerUser(String username, String password) throws RemoteException, NullPointerException;
}

