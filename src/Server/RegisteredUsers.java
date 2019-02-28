package Server;

import Utils.IntRegistration;

import java.rmi.server.RemoteServer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RegisteredUsers è una collezione contenente i vari utenti registrati a TURING.
 */
public class RegisteredUsers extends RemoteServer implements IntRegistration
{
    // hashmap di tipo <username, info dell'utente>
    private ConcurrentHashMap<String, UserInfo> users;

    RegisteredUsers()
    {
        this.users = new ConcurrentHashMap<>();
    }

    /**
     * Funzione che permette la registrazione di un utente a TURING, aggiungendolo alla collezione 'users'.
     *
     * @param username username dell'utente da registrare
     * @param password password dell'utente da registrare
     *
     * @return true se la registrazione va a buon fine,
     *         false se esiste già un utente registrato con lo stesso username
     *
     * @throws NullPointerException se username/password sono NULL
     */
    public boolean registerUser(String username, String password) throws NullPointerException
    {
        if(username == null || password == null)
            throw new NullPointerException();

        if(users.containsKey(username))
            return false;

        UserInfo u = new UserInfo(password);
        users.putIfAbsent(username, u);
        return true;
    }

    /**
     * Funzione che permette di ottenere le informazioni di un utente.
     *
     * @param username username dell'utente
     *
     * @return oggetto UserInfo
     */
    UserInfo getUser(String username)
    {
        return users.get(username);
    }

    /**
     * Funzione che imposta lo stato (online / offline) di un utente.
     *
     * @param username username dell'utente
     * @param password password dell'utente
     * @param online stato da impostare { 1 = online, 0 = offline }
     * @return true se lo stato è impostato correttamente
     *         false altrimenti
     */
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

    /**
     * Funzione che controlla la consistenza e l'esistenza di un username e relativa password.
     *
     * @param username username da controllare
     * @param password password associata all'username
     *
     * @return true se i dati sono consistenti e corretti,
     *         false altrimenti
     */
    private boolean checkData(String username, String password)
    {
        return (username != null && password != null && users.containsKey(username));
    }
}
