package Client;

import GUI.FrameCode;
import GUI.MyFrame;
import GUI.TuringPanel;
import Utils.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    /**
     * Metodo main del client
     */
    public static void main(String[] args)
    {
        //creo il frame di login
        new MyFrame(null, FrameCode.LOGIN);

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
     * @param socketChannel il socket su cui mandare la richiesta
     */
    public static void sendReq(SocketChannel socketChannel, Operation op)
    {
        try
        {
            Utils.sendObject(socketChannel,op);
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
    public static OpCode getAnswer()
    {
        OpCode answerCode;
        try
        {
            byte[] answerBytes = Utils.recvBytes(clientSocketChannel);

            if(answerBytes.length == 0)
                answerCode = OpCode.OP_FAIL;
            else
                answerCode = OpCode.valueOf(new String(answerBytes));

        }
        catch (IOException | NullPointerException e)
        {
            System.err.println("Error in reading operation code result");
            e.printStackTrace();
            answerCode = OpCode.OP_FAIL;
        }

        return answerCode;
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
    public static OpCode register()
    {
        IntRegistration registration;
        Remote remote;

        try
        {
            Registry registry = LocateRegistry.getRegistry(Utils.ADDRESS, Utils.REGISTRATION_PORT);
            remote = registry.lookup("TURING-SERVER");
            registration = (IntRegistration) remote;
        }
        catch(Exception e)
        {
            System.err.println("Error in invoking registry");
            e.printStackTrace();
            return OpCode.OP_FAIL;
        }

        try
        {
            if(registration.registerUser(username,password))
            {
                /* effettuo il login in modo da aprire le socket necessarie alla comunicazione col server
                   e risultare "online"
                 */
                loginUser();
                return OpCode.OP_OK;
            }
            else
                return OpCode.ERR_USER_ALREADY_LOGGED;
        }
        catch(RemoteException | NullPointerException e)
        {
            System.err.println("Error in invoking method \"registerUser\" " + e.toString());
            e.printStackTrace();
            return OpCode.OP_FAIL;
        }
    }

    /**
     * Funzione che effettua il login al servizio TURING,
     * aprendo le socket e istanziando il thread necessario alla ricezione degli inviti.
     *
     * @return esito della richiesta di login,
     *         OP_FAIL in caso di errore
     */
    public static OpCode loginUser()
    {
        OpCode answerCode;

        clientSocketChannel = openConnection(Utils.CLIENT_PORT);

        if(clientSocketChannel == null)
            answerCode = OpCode.OP_FAIL;
        else
        {
            //effettuo la richiesta di login
            Operation request = new Operation(username);
            request.setPassword(password);
            request.setCode(OpCode.LOGIN);

            sendReq(clientSocketChannel,request);
            answerCode = getAnswer();

            //se il login è andato a buon fine, inizializzo il thread per ricevere gli inviti
            if(answerCode == OpCode.OP_OK)
            {
                // apro un nuovo socket per ricevere gli inviti
                inviteSocketChannel = openConnection(Utils.CLIENT_PORT);
                if(inviteSocketChannel == null)
                {
                    //in caso di errore chiudo tutte le socket
                    try {clientSocketChannel.close();}
                    catch (IOException e) {e.printStackTrace();}
                    return OpCode.OP_FAIL;
                }

                request.setCode(OpCode.SET_INVITATION_SOCK);
                sendReq(inviteSocketChannel, request);

                //creo il thread per la ricezione degli inviti
                inviteThread = new Thread(new InviteTask(inviteSocketChannel));
                inviteThread.start();

                //richiedo e scarico la lista di eventuali inviti ricevuti mentre ero offline
                request.setCode(OpCode.PENDING_INVITATIONS);
                sendReq(clientSocketChannel,request);

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
                    answerCode = OpCode.OP_FAIL;
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
    public static OpCode logoutUser()
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(OpCode.LOGOUT);

        sendReq(clientSocketChannel,request);

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


        return OpCode.OP_OK;
    }

    /**
     * Funzione per creare un nuovo documento su TURING.
     *
     * @param name nome del documento
     * @param nsection numero delle sezioni del documento
     * @return esito della richiesta di creazione,
     *         OP_FAIL in caso di errore
     */
    public static OpCode createDocument(String name, int nsection)
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(OpCode.CREATE);

        request.setFilename(name);
        request.setSection(nsection);

        sendReq(clientSocketChannel,request);

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
    public static OpCode manageDocument(OpCode code, String name, String owner, int nsection)
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(code);
        request.setFilename(name);
        request.setOwner(owner);
        request.setSection(nsection);

        sendReq(clientSocketChannel,request);
        OpCode answerCode = getAnswer();

        //se voglio modificare una sezione, e la richiesta è andata a buon fine...
        if(code == OpCode.EDIT && answerCode == OpCode.OP_OK)
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
                answerCode = OpCode.OP_FAIL;
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
    public static OpCode recvSection(String name, String owner, int section)
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(OpCode.SECTION_RECEIVE);
        request.setFilename(name);
        request.setOwner(owner);
        request.setSection(section);
        sendReq(clientSocketChannel,request);

        OpCode answerCode;

        try
        {
            Utils.transferFromSection(clientSocketChannel, username,false);
            answerCode = getAnswer();
        }
        catch(IOException ioe)
        { answerCode = OpCode.OP_FAIL; }

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
    public static OpCode endEditDocument(String name, String owner, int section)
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(OpCode.END_EDIT);
        request.setFilename(name);
        request.setOwner(owner);
        request.setSection(section);

        sendReq(clientSocketChannel,request);

        OpCode answerCode;

        try
        {
            /* mando la sezione al server, indicando il path in cui salvarla, ovvero:
               "username/filename/filename_section.txt"
             */
            String filepath = Utils.getPath(username,name,section,false).replaceFirst("./","");
            Utils.transferToSection(clientSocketChannel,filepath);
            answerCode = getAnswer();

            // elimino il file sul client dopo aver mandato il suo aggiornamento al server
            Files.deleteIfExists(Paths.get(filepath));
        }
        catch(IOException ioe)
        { answerCode = OpCode.OP_FAIL; }

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
    public static OpCode invite(String filename, String collaborator)
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(OpCode.INVITE);
        request.setFilename(filename);
        request.setOwner(collaborator);

        sendReq(clientSocketChannel,request);
        return getAnswer();
    }

}
