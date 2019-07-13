package Server;

import GUI.FrameCode;
import GUI.MyFrame;
import GUI.ServerPanel;
import Utils.*;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class MainServer
{
    /**
     * Funzione per generare un indirizzo IP multicast (il primo ottetto è fissato a 239
     *
     * @return Stringa che rappresenta indirizzo IP multicast nel formato deciame xxx.xxx.xxx.xxx
     */
    private static String generateMulticastAddress()
    {
        String address = "239";

        for (int i = 1; i <= 3; i++)
            address += "." + ( (int) (Math.random() * 256));

        return address;

    }

    // collezione contenente gli indirizzi multicast già assegnati
    static HashMap<String, Boolean> usedAddresses = new HashMap<>();

    /**
     * Metodo main del server
     */
    public static void main(String[] args)
    {
        // collezione degli utenti registrati a TURING
        RegisteredUsers registeredUsers = new RegisteredUsers();

        /* collezione dei file caricati dagli utenti su TURING, nel formato
           < filename_owner , info del documento >
         */
        ConcurrentHashMap<String, FileInfo> userFiles = new ConcurrentHashMap<>();

        // finestra di log degli eventi all'interno del server
        new MyFrame(null, FrameCode.SERVER);

        // Stringa contenente il messaggio di log da visualizzare nella finestra degli eventi.
        String log;

        // riferimento all'area di testo della finestra di log
        JTextPane pane = ServerPanel.logPane;

        /* Creazione del registry per poter fornire la funzione di registrazione al servizio TURING" */
        try
        {
            IntRegistration stub = (IntRegistration) UnicastRemoteObject.exportObject(registeredUsers,0);

            LocateRegistry.createRegistry(Utils.REGISTRATION_PORT);
            Registry r = LocateRegistry.getRegistry(Utils.ADDRESS, Utils.REGISTRATION_PORT);

            r.rebind("TURING-SERVER",stub);
        }
        catch (RemoteException e)
        {
            String msg = "Communication error";
            Utils.appendToPane(pane,msg+"\n", Color.RED,true);
            System.err.println(msg + " " + e.toString());
        }

        /* -------------------------------------------- */

        /* Creo una funzione di cleanup, che al termine dell'esecuzione del server (anche accidentale)
        elimina la directory contenente tutti i file degli utenti.
         */

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    try { Utils.deleteDirectory(Utils.SERVER_DIR_PATH); }
                    catch(IOException ioe)
                    { System.err.println("Can't delete " + Utils.SERVER_DIR_PATH); }
                }));

        /* Creazione del socket TCP per la ricezione delle richieste dei client
        *  e invio degli esiti di tali richieste */

        ServerSocketChannel clientSSC;
        Selector selector;

        try
        {
            clientSSC = ServerSocketChannel.open();
            ServerSocket clientServerSocket = clientSSC.socket();
            clientServerSocket.bind(new InetSocketAddress(Utils.ADDRESS, Utils.CLIENT_PORT));

            log = "Server opened on port " + Utils.CLIENT_PORT;
            Utils.appendToPane(pane,log+"\n",Color.BLUE,false);
            System.out.println(log);

            // registro il socket TCP al selettore, come socket su cui accettare richieste
            clientSSC.configureBlocking(false);
            selector = Selector.open();
            clientSSC.register(selector, SelectionKey.OP_ACCEPT);
        }
        catch (IOException ioe)
        {
            System.err.println("Error opening server socket for client reqeusts " + ioe.toString() + ioe.getMessage());
            ioe.printStackTrace();
            return;
        }


        while (true)
        {
            try { selector.select(); }
            catch (IOException ioe)
            {
                log = "Error on select";
                Utils.appendToPane(pane,log+"\n",Color.RED,true);
                System.err.println(log + " " + ioe.toString() + ioe.getMessage());
                ioe.printStackTrace();
                break;
            }

            //ottengo l'insieme delle chiavi pronte a operazioni di I/O
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while (iterator.hasNext())
            {
                SelectionKey key = iterator.next();

                try
                {
                    // sto accettando una nuova connessione
                    if(key.isAcceptable())
                    {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        // registro la connessione in ingresso come socket da cui leggere una richiesta
                        client.register(selector, SelectionKey.OP_READ);

                        log = "New connection from " + client.getRemoteAddress();
                        Utils.appendToPane(pane,log+"\n",Color.BLUE,false);
                        System.out.println(log);
                    }
                    else if(key.isReadable()) // sto per leggere da una socket
                    {
                        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
                        Operation op_in;
                        OpCode answerCode = OpCode.OP_FAIL;

                        // leggo la richiesta del client
                        try
                        {
                            op_in = (Operation) Utils.recvObject(clientSocketChannel);
                            if(op_in == null)
                                throw new NullPointerException();
                        }
                        catch(ClassNotFoundException e)
                        {
                            log = "Error on reading Operation";
                            Utils.appendToPane(pane,log+"\n",Color.RED,true);
                            System.err.println(log + " " + e.toString() + " " + e.getMessage());
                            e.printStackTrace();

                            // in caso di errore sovrascrivo il codice con OP_FAIL
                            answerCode = OpCode.OP_FAIL;
                            key.attach(answerCode);
                            key.interestOps(SelectionKey.OP_WRITE);
                            continue;
                        }
                        catch(NullPointerException e) // se non leggo nulla dal socket chiudo questa connessione
                        {
                            key.cancel();
                            clientSocketChannel.close();
                            continue;
                        }

                        String usr = op_in.getUsername();
                        String psw = op_in.getPassword();

                        /* controllo il tipo di richiesta ricevuta.
                           Per i vari codici operazione vedi la enum 'OpCode'
                          */
                        switch (op_in.getCode())
                        {

                            case PENDING_INVITATIONS:
                            {
                                UserInfo userInfo = registeredUsers.getUser(usr);

                                // mando inviti pendenti all'utente
                                Utils.sendObject(clientSocketChannel,userInfo.getPendingInvitations());

                                // pulisco la lista degli inviti pendenti
                                userInfo.clearPendingInvites();

                                answerCode = OpCode.OP_OK;
                                break;
                            }

                            case FILE_LIST:
                            {
                                String owner = op_in.getOwner();

                                /* se owner = username,
                                   allora mando solo i file di cui l'utente che ha fatto la richiesta è proprietario.
                                  */
                                boolean onlyMyFiles = usr.equals(owner);

                                /* mando all'utente la lista di file che può gestire,
                                   impostando l'esito di tale invio, che manderò successivamente
                                 */
                                try
                                {
                                    ArrayList<String> namesToSend = new ArrayList<>();
                                    for (String s: registeredUsers.getUser(usr).getFiles())
                                    {
                                        /* costruisco l'opportuno pattern che il client leggerà:
                                           < nomefile_owner_numsezioni >
                                         */

                                        if(!onlyMyFiles || userFiles.get(s).getOwner().equals(owner))
                                            namesToSend.add(s + "_" + userFiles.get(s).getNsections());
                                    }
                                    Utils.sendObject(clientSocketChannel, namesToSend);
                                    answerCode = OpCode.OP_OK;
                                }
                                catch(IOException ioe)
                                { answerCode = OpCode.OP_FAIL; }
                                break;
                            }

                            case SET_INVITATION_SOCK:
                            {
                                UserInfo userInfo = registeredUsers.getUser(usr);

                                /* salvo il socket che userà il client per ricevere gli inviti */
                                userInfo.setInviteSocketChannel(clientSocketChannel);

                                log = "New connection (invitation)";
                                Utils.appendToPane(pane,log+"\n",Color.GREEN,false);
                                System.out.println(log);

                                /* cancello il socket degli inviti dal selettore,
                                in quanto non mi aspetto altre richieste di scrittura da esso
                                 */
                                key.cancel();
                                continue;
                            }

                            case LOGIN:
                            {
                                UserInfo userInfo = registeredUsers.getUser(usr);

                                if(userInfo == null)
                                    answerCode = OpCode.ERR_USER_UNKNOWN;
                                else if(!userInfo.getPassword().equals(psw))
                                    answerCode = OpCode.ERR_WRONG_PASSWORD;
                                else if(userInfo.isOnline())
                                    answerCode = OpCode.ERR_USER_ALREADY_LOGGED;
                                else if(registeredUsers.setStatus(usr,psw,1))
                                {
                                    /* creo la directory (solo se non esiste già)
                                       che conterrà tutti i file creati da questo utente */
                                    Files.createDirectories(Paths.get(Utils.SERVER_DIR_PATH + usr));

                                    answerCode = OpCode.OP_OK;
                                }
                                else
                                    answerCode = OpCode.OP_FAIL;
                                break;
                            }

                            case LOGOUT:
                            {
                                if(registeredUsers.setStatus(usr,psw,0))
                                {
                                    UserInfo userInfo = registeredUsers.getUser(usr);

                                    String editingFilename = userInfo.getEditingFilename();
                                    int editingSection = userInfo.getEditingSection();

                                    if(!editingFilename.equals(""))
                                    {
                                        // se l'utente stava editando una sezione, la sblocco
                                        FileInfo fileInfo = userFiles.get(editingFilename);
                                        fileInfo.unlockSection(editingSection - 1);

                                        // decremento il numero di collaboratori sul file
                                        fileInfo.decCounterEditors();
                                    }

                                    Utils.printLog(usr,op_in.getCode(), OpCode.OP_OK);

                                    //chiudo le due socket del client
                                    key.cancel();
                                    clientSocketChannel.close();
                                    userInfo.getInviteSocketChannel().close();
                                    continue;
                                }
                                break;
                            }

                            case CREATE:
                            {
                                String filename = op_in.getFilename();
                                String collectionFileName = filename + "_" + usr;
                                int nsections = op_in.getSection();

                                // controllo che non esista un file con lo stesso nome, creato dallo stesso utente
                                if(userFiles.containsKey(collectionFileName))
                                {
                                    answerCode = OpCode.ERR_FILE_ALREADY_EXISTS;
                                }
                                else // se non esiste, aggiungo il file alla collezione gestita dal server
                                {
                                    // creo la directory che conterrà le sezioni del documento
                                    Files.createDirectories(Paths.get(Utils.getPath(usr,filename,0,true)));

                                    boolean err = false;

                                    // creo le sezioni del documento
                                    for (int i = 1; i <= nsections; i++)
                                    {
                                        try
                                        {
                                            Files.createFile(Paths.get(Utils.getPath(usr,filename,i,true)));
                                        }
                                        catch(IOException ioe)
                                        {
                                            err = true;
                                            break;
                                        }
                                    }

                                    // se non sono avvenuti errori in fase di creazione del file
                                    if(!err)
                                    {
                                        //genero un indirizzo di multicast da assegnare al file che sto per creare

                                        //ne cerco uno utilizzabile tra quelli precedentemente generati
                                        String address = null;
                                        for (Map.Entry<String,Boolean> entry: usedAddresses.entrySet())
                                        {
                                            if (entry.getValue() == Boolean.FALSE)
                                            {
                                                address = entry.getKey();
                                                break;
                                            }
                                        }

                                        //se non ne trovo uno, lo creo
                                        if (address == null)
                                        {
                                            do
                                            { address = generateMulticastAddress(); }
                                            while(usedAddresses.containsKey(address));
                                        }

                                        //creo la struttura dati contenente le info del file
                                        FileInfo fileInfo = new FileInfo(usr,nsections,address);

                                        //aggiungo l'indirizzo generato tra quelli in uso
                                        usedAddresses.put(address, Boolean.TRUE);

                                        fileInfo.setOwner(usr);
                                        userFiles.putIfAbsent(collectionFileName, fileInfo);


                                        //aggiungo il file alla lista di quelli gestibili dall'utente che l'ha creato
                                        registeredUsers.getUser(usr).addFile(collectionFileName);
                                        answerCode = OpCode.OP_OK;
                                    }
                                    else // se ci sono stati errori, elimino la directory appena creata
                                    {
                                        Utils.deleteDirectory(Utils.getPath(usr,filename,0,true));
                                        answerCode = OpCode.OP_FAIL;
                                    }
                                }
                                break;
                            }

                            case SHOW_ALL:
                            case SHOW:
                            case EDIT:
                            {
                                String filename = op_in.getFilename();
                                String owner = op_in.getOwner();
                                String collectionFilename = filename + "_" + owner;
                                int section = op_in.getSection();

                                UserInfo userInfo = registeredUsers.getUser(usr);
                                FileInfo fileInfo = userFiles.get(collectionFilename);

                                if(userInfo.canEdit(collectionFilename)) //utente con permessi
                                {
                                    /* SHOW_ALL richiede tutte le sezioni (successivamente) , quindi non effettuo
                                       ulteriori controlli sul documento
                                      */
                                    if(!op_in.getCode().equals(OpCode.SHOW_ALL))
                                    {
                                        if(!fileInfo.isLocked(section-1)) //sezione non lockata
                                        {
                                            // se la richiesta è di editing ...
                                            if(op_in.getCode().equals(OpCode.EDIT))
                                            {
                                                // ... lock sulla sezione
                                                fileInfo.lockSection(section-1);

                                                // salvo qual è il documento che l'utente sta modificando
                                                userInfo.setEditingFilename(collectionFilename);
                                                userInfo.setEditingSection(section);

                                                // mando all'utente l'esito positivo della richiesta di EDIT
                                                Utils.sendBytes(clientSocketChannel, OpCode.OP_OK.toString().getBytes());

                                                // mando all'utente l'indirizzo di multicast associato al file
                                                Utils.sendBytes(clientSocketChannel,fileInfo.getAddress().getBytes());

                                                // aumento il numero di collaboratori del documento
                                                fileInfo.incCounterEditors();
                                            }
                                            answerCode = OpCode.OP_OK;
                                        }
                                        else
                                            answerCode = OpCode.SECTION_EDITING;
                                    }
                                    else
                                        answerCode = OpCode.OP_OK;
                                }
                                else
                                    answerCode = OpCode.ERR_PERMISSION_DENIED;
                                break;
                            }

                            case SECTION_RECEIVE:
                            {
                                String filename = op_in.getFilename();
                                String owner = op_in.getOwner();
                                int section = op_in.getSection();
                                String collectionFilename = filename + "_" + owner;

                                /* trasferisco la sezione richiesta all'utente
                                   (se sono arrivato qui, ho già effettuato i vari controlli
                                   su mutua esclusione e permessi) */
                                try
                                {
                                    Utils.transferToSection(clientSocketChannel,Utils.getPath(owner,filename,section,true).replaceFirst("./",""));
                                    // segnalo all'utente se tale sezione è in fase di editing
                                    if(userFiles.get(collectionFilename).isLocked(section-1))
                                        answerCode = OpCode.SECTION_EDITING;
                                    else
                                        answerCode = OpCode.OP_OK;

                                }
                                catch(IOException ioe)
                                {
                                    answerCode = OpCode.OP_FAIL;
                                }
                                break;
                            }

                            case END_EDIT:
                            {
                                String filename = op_in.getFilename();
                                String owner = op_in.getOwner();
                                String collectionFilename = filename + "_" + owner;
                                int section = op_in.getSection();

                                // ricevo la sezione aggiornata dall'utente
                                try
                                {
                                    Utils.transferFromSection(clientSocketChannel,usr,true);

                                    // tengo traccia del fatto che l'utente non sta più editando
                                    registeredUsers.getUser(usr).setEditingFilename("");
                                    registeredUsers.getUser(usr).setEditingSection(0);

                                    FileInfo fileInfo = userFiles.get(collectionFilename);

                                    // sblocco la sezione che l'utente stava editando
                                    fileInfo.unlockSection(section-1);

                                    // decremento il numero di collaboratori sul file che l'utente stava editando
                                    fileInfo.decCounterEditors();

                                    answerCode = OpCode.OP_OK;
                                }
                                catch(IOException ioe)
                                {
                                    System.err.println("Error downloading section " + ioe.toString());
                                    ioe.printStackTrace();
                                    answerCode = OpCode.OP_FAIL;
                                    break;
                                }
                                break;
                            }

                            case INVITE:
                            {
                                String filename = op_in.getFilename();
                                String owner = op_in.getOwner();
                                String collectionFilename = filename + "_" + usr;


                                UserInfo userInfo = registeredUsers.getUser(owner);

                                if(userInfo == null) // utente inesistente
                                    answerCode = OpCode.ERR_USER_UNKNOWN;
                                else if(userInfo.canEdit(collectionFilename)) // controllo che l'utente non sia già stato invitato
                                {
                                    answerCode = OpCode.ERR_USER_ALREADY_INVITED;
                                }
                                else
                                {
                                    userInfo.addFile(collectionFilename);

                                    // utente non online, aggiungo l'invito alla sua lista di inviti pendenti
                                    if(!userInfo.isOnline())
                                    {
                                        Message invitation = new Message(usr, filename, new Date());
                                        userInfo.addPendingInvite(invitation);
                                    }
                                    else // utente online, gli mando l'invito in tempo reale
                                    {
                                        Message invitation = new Message(usr, filename, new Date());
                                        Utils.sendObject(userInfo.getInviteSocketChannel(), invitation);
                                    }

                                    answerCode = OpCode.OP_OK;
                                }
                                break;
                            }

                            default:
                                break;
                        }

                        key.attach(answerCode);
                        key.interestOps(SelectionKey.OP_WRITE);

                        Utils.printLog(usr,op_in.getCode(),answerCode);
                    }
                    else if (key.isWritable()) //sto per scrivere su una socket
                    {
                        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
                        OpCode answerCode = (OpCode) key.attachment();

                        // scrivo l'esito della richiesta fatta dall'utente
                        Utils.sendBytes(clientSocketChannel,answerCode.toString().getBytes());
                        key.interestOps(SelectionKey.OP_READ);
                    }
                }
                catch (IOException ex)
                {
                    key.cancel();
                    try { key.channel().close(); }
                    catch (IOException ioe) {ioe.printStackTrace(); break;}
                }
                iterator.remove();
            }
        }
    }
}