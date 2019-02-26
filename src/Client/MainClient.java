package Client;

import GUI.MyFrame;
import GUI.TuringPanel;
import GUI.frameCode;

import Utils.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;


public class MainClient
{
    // socket TCP per la trasmissione delle richieste e dei loro esiti.
    public static SocketChannel clientSocketChannel;

    // socket TCP e thread per la ricezione di inviti da parte di altri utenti.
    private static SocketChannel inviteSocketChannel;
    private static Thread inviteThread;

    /* thread per la chat in tempo reale con altri utenti che collaborano all'editing
       del documento che il client sta modificando
     */
    private static Thread chatThread;

    // username e password del client, ottenuti dal pannello di login
    public static String username;
    public static String password;

    // lista di inviti pendenti, inizializzata al momento del login
    public static ArrayList<Message> pendingInvitations;


    public static void main(String[] args)
    {
        //creo il frame di login
        new MyFrame(null, frameCode.LOGIN);

        /* imposto una funzione di cleanup che elimina i file dell'utente
           alla terminazione (anche involontaria) del client
         */
        Runtime.getRuntime().addShutdownHook(
                new Thread(() ->
                {
                    try
                    { Utils.deleteDirectory(Utils.CLIENT_FILES_PATH + username); }
                    catch(IOException ioe) {System.err.println("Can't delete " + Utils.CLIENT_FILES_PATH + username); }

                    logoutUser();
                }));
    }

    /**
     * Procedura per effettuare l'invio di una richiesta al server
     *
     * @param op contiene il codice della richiesta ed eventuali parametri
     */
    public static void sendReq(Operation op)
    {
        try
        {
            Utils.sendObject(clientSocketChannel,op);
        }
        catch(NullPointerException npe)
        {
            System.err.println("Connection not yet open");
        }
        catch(ClosedChannelException e)
        {
            System.err.println("Closed TCP Socket");
        }
        catch(IOException ioe)
        {
            System.err.println("Error sending operation " + op.getCode());
            ioe.printStackTrace();
        }

    }


    /**
     * Procedura per ricevere l'esito di una richiesta
     *
     * @return esito della richiesta, in caso di ricezione andata a buon fine
     *         OP_FAIL altrimenti
     */
    public static opCode getAnswer()
    {
        try
        {
            byte[] answerBytes = Utils.recvBytes(clientSocketChannel);

            if(answerBytes.length == 0)
                return opCode.OP_FAIL;
            else
                return opCode.valueOf(new String(answerBytes));

        }
        catch (IOException | NullPointerException e)
        {
            System.err.println("Error in reading operation code result");
            e.printStackTrace();
            return opCode.OP_FAIL;
        }
    }


    /**
     * Inizializza una socket TCP, sulla porta passata come argomento
     *
     * @param port numero di porta
     * @return la socket aperta;
     *         null, in caso di errore
     */
    private static SocketChannel openConnection(int port)
    {
        SocketChannel socketChannel;
        InetSocketAddress address = new InetSocketAddress(Utils.ADDRESS, port);
        try
        {
            socketChannel = SocketChannel.open();
            socketChannel.connect(address);

            //aspetto che termini la connessione
            while (!socketChannel.finishConnect())
            {continue;}
        }
        catch(IOException ioe)
        {
            System.err.println("Error in opening socket");
            ioe.printStackTrace();
            return null;
        }

        return socketChannel;
    }

    /**
     * Funzione per richiedere la registrazione al servizio TURING
     *
     * @return
     *      OP_OK, in caso di registrazione andata a buon fine
     *      ERR_USER_ALREADY_LOGGED, se esiste già un utente registrato con lo stesso username
     *      OP_FAIL in caso di errore
     */
    public static opCode register()
    {
        IntRegistration registration;
        Remote remote;

        try
        {
            int REGISTRATION_PORT = 5000;
            Registry registry = LocateRegistry.getRegistry(REGISTRATION_PORT);
            remote = registry.lookup("TURING-SERVER");
            registration = (IntRegistration) remote;
        }
        catch(Exception e)
        {
            System.err.println("Error in invoking registry");
            e.printStackTrace();
            return opCode.OP_FAIL;
        }

        try
        {
            if(registration.registerUser(username,password))
            {
                /* effettuo il login in modo da aprire le socket necessarie alla comunicazione col server
                   e risultare "online"
                 */
                loginUser();
                return opCode.OP_OK;
            }
            else
                return opCode.ERR_USER_ALREADY_LOGGED;
        }
        catch(RemoteException re)
        {
            System.err.println("Error in invoking method \"registerUser\" ");
            re.printStackTrace();
            return opCode.OP_FAIL;
        }
    }

    /**
     * Funzione che effettua il login al servizio TURING,
     * aprendo le socket e istanziando il thread necessario alla ricezione degli inviti.
     *
     * @return esito della richiesta di login,
     *         OP_FAIL in caso di errore
     */
    public static opCode loginUser()
    {
        opCode answerCode;

        clientSocketChannel = openConnection(Utils.CLIENT_PORT);

        if(clientSocketChannel == null)
            answerCode = opCode.OP_FAIL;
        else
        {
            //effettuo la richiesta di login
            Operation request = new Operation(username);
            request.setPassword(password);
            request.setCode(opCode.LOGIN);
            sendReq(request);

            //apro la socket degli inviti
            inviteSocketChannel = openConnection(Utils.INVITE_PORT);
            if(inviteSocketChannel == null)
            {
                //in caso di errore chiudo tutte le socket
                try {clientSocketChannel.close();}
                catch (IOException e) {e.printStackTrace();}
                answerCode = opCode.OP_FAIL;
            }
            else
            {
                answerCode = getAnswer();

                //se il login è andato a buon fine, inizializzo il thread per ricevere gli inviti
                if(answerCode == opCode.OP_OK)
                {
                    //creo il thread per la ricezione degli inviti
                    inviteThread = new Thread(new InviteTask(inviteSocketChannel));
                    inviteThread.start();

                    //richiedo e scarico la lista di eventuali inviti ricevuti mentre ero offline
                    request.setCode(opCode.PENDING_INVITATIONS);
                    sendReq(request);

                    try
                    {
                        pendingInvitations = (ArrayList<Message>) Utils.recvObject(clientSocketChannel);

                        if(pendingInvitations == null)
                            throw new NullPointerException();

                        //esito dell'operazione di login
                        answerCode = getAnswer();
                    }
                    catch(ClassNotFoundException | IOException | NullPointerException e)
                    {
                        System.err.println("Error in downloading pending invites");
                        answerCode = opCode.OP_FAIL;
                    }
                }
            }
        }
        return answerCode;
    }


    /**
     * Funzione che effettua il logout dal servizio TURING, chiudendo tutte le connessioni verso di esso
     * e terminando i thread in esecuzione sul client (inviti e chat).
     *
     * @return OP_OK
     */
    public static opCode logoutUser()
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(opCode.LOGOUT);

        sendReq(request);

        //chiudo socket del client
        if(clientSocketChannel != null)
        {
            try { clientSocketChannel.close(); }
            catch(IOException ioe)
            {
                System.err.println("Error in closing clientSocketChannel");
                ioe.printStackTrace();
            }
        }

        //interrompo il thread degli inviti
        if(inviteThread != null)
            inviteThread.interrupt();


        //chiudo socket degli inviti
        if(inviteSocketChannel != null)
        {
            try { inviteSocketChannel.close(); }
            catch(IOException ioe)
            {
                System.err.println("Error in closing inviteSocketChannel");
                ioe.printStackTrace();
            }
        }

        //mando un messaggio agli altri utenti, per notificare la mia disconnessione
        Utils.sendChatMessage(username, "left the chat", TuringPanel.editingFileAddress,null);

        //interrompo il thread della chat
        if(chatThread != null)
            chatThread.interrupt();


        return opCode.OP_OK;
    }

    /**
     * Funzione per creare un nuovo documento su TURING.
     *
     * @param name nome del documento
     * @param nsection numero delle sezioni del documento
     * @return esito della richiesta di creazione,
     *         OP_FAIL in caso di errore
     */
    public static opCode createDocument(String name, int nsection)
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(opCode.CREATE);

        request.setFilename(name);
        request.setSection(nsection);

        sendReq(request);

        return getAnswer();
    }

    /**
     * Funzione per richiere la modifica/visualizzazione di un documento o di una sua sezione.
     *
     * @param code codice della richiesta da effettuare (EDIT, SHOW, SHOW_ALL)
     * @param name nome del documento
     * @param owner proprietario del documento
     * @param nsection numero della sezione da voler gestire (se code = EDIT/SHOW)
     *                 numero di sezioni del documento (se code = SHOW_ALL)
     *
     * @return esito della richiesta di visualizzazione/modifica
     *         OP_FAIL in caso di errore
     */
    public static opCode manageDocument(opCode code, String name, String owner, int nsection)
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(code);
        request.setFilename(name);
        request.setOwner(owner);
        request.setSection(nsection);

        sendReq(request);
        opCode answerCode = getAnswer();

        //se voglio modificare una sezione, e la richiesta è andata a buon fine...
        if(code == opCode.EDIT && answerCode == opCode.OP_OK)
        {
            String address = null;

            //... ricevo l'indirizzo di multicast assegnato al documento che ho chiesto di modificare
            try
            {
                address = new String(Utils.recvBytes(clientSocketChannel));
                TuringPanel.editingFileAddress = address;
                answerCode = getAnswer();
            }
            catch(IOException e)
            {
                System.err.println("Can't download multicast address");
                answerCode = opCode.OP_FAIL;
            }

            /* creo il thread che si occuperò della chat con gli altri utenti
               che modificano altre sezioni dello stesso documento
             */
            chatThread = new Thread(new ChatTask(address, Utils.MULTICAST_PORT));
            chatThread.start();
        }
        return answerCode;
    }

    /**
     * Funzione per effettuare la richiesta di ricezione, e successivo download,
     * di una sezione di un documento.
     *
     * @param name nome del documento
     * @param owner proprietario del documento
     * @param section
     * @return
     */
    public static opCode recvSection(String name, String owner, int section)
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(opCode.SECTION_RECEIVE);
        request.setFilename(name);
        request.setOwner(owner);
        request.setSection(section);
        sendReq(request);

        opCode answerCode;

        try
        {
            Utils.transferFromSection(clientSocketChannel, username,false);
            answerCode = getAnswer();
        }
        catch(IOException ioe)
        { answerCode = opCode.OP_FAIL; }

        return answerCode;


    }

    /**
     * Funzione per notificare la fine dell'editing di una sezione di un documento
     * e invio della versione aggiornata a TURING.
     *
     * @param name nome del documento
     * @param owner proprietario del documento
     * @param section numero della sezione
     *
     * @return esito della richiesta di fine editing e di upload,
     *         OP_FAIL in caso di errore
     */
    public static opCode endEditDocument(String name, String owner, int section)
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(opCode.END_EDIT);
        request.setFilename(name);
        request.setOwner(owner);
        request.setSection(section);

        sendReq(request);

        opCode answerCode;

        try
        {
            /* mando la sezione al server, indicando il path in cui salvarla, ovvero:
               "username/filename/filename_section.txt"
             */
            String filepath = Utils.getPath(username,name,section,false).replaceFirst("./","");
            Utils.transferToSection(clientSocketChannel,filepath);
            answerCode = getAnswer();
        }
        catch(IOException ioe)
        { answerCode = opCode.OP_FAIL; }

        return answerCode;
    }

    /**
     * Funzione per invitare un altro utente a modificare un documento.
     *
     * @param filename nome del documento
     * @param collaborator nome dell'utente che si vuole invitare
     *
     * @return esito della richiesta di invito
     */
    public static opCode invite(String filename, String collaborator)
    {
        Operation request = new Operation(collaborator);
        request.setPassword(password);
        request.setCode(opCode.INVITE);
        request.setFilename(filename);
        request.setOwner(username);

        sendReq(request);
        return getAnswer();
    }

}
