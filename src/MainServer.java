import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;


public class MainServer
{
    private static String getPath(String username, String filename, int section)
    {
        return Utils.SERVER_FILES_PATH + username + "/" + filename + ((section == 0) ? "" : ("/" + filename + section + ".txt"));
    }

    public static void main(String[] args)
    {
        //collezione degli utenti registrati a TURING
        RegisteredUsers registeredUsers = new RegisteredUsers();
        //collezione dei file caricati dagli utenti su TURING
        ConcurrentHashMap<String, FileInfo> userFiles = new ConcurrentHashMap<>();


        /* Creazione del registry per poter fornire la funzione di "registrazione/deregistrazione" al servizio TURING" */
        try
        {
            IntRegistration stub = (IntRegistration) UnicastRemoteObject.exportObject(registeredUsers,0);

            int REGISTRATION_PORT = 5000;
            LocateRegistry.createRegistry(REGISTRATION_PORT);
            Registry r=LocateRegistry.getRegistry(REGISTRATION_PORT);

            r.rebind("TURING-SERVER",stub);
        }
        catch (RemoteException e)
        { System.err.println("Communication error " + e.toString()); }

        /* -------------------------------------------- */

        /* Creo una funzione di cleanup, che al termine dell'esecuzione del server
           elimina la directory contenente tutti i file degli utenti.
         */

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    try { Utils.deleteDirectory(Utils.SERVER_FILES_PATH); }
                    catch(IOException ioe) {System.err.println("Can't delete " + Utils.SERVER_FILES_PATH); }
                }));

        /* Creazione del socket TCP per tutte le altre richieste dei client */

        ServerSocketChannel serverSocketChannel;
        Selector selector;
        try
        {
            serverSocketChannel = ServerSocketChannel.open();
            ServerSocket serverSocket = serverSocketChannel.socket();
            int PORT = 5001;
            serverSocket.bind(new InetSocketAddress("127.0.0.1", PORT));
            System.out.println("Server aperto su porta " + PORT);

            serverSocketChannel.configureBlocking(false);
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        }
        catch (IOException ioe)
        {
            System.err.println("Error opening TCP socket " + ioe.toString() + ioe.getMessage());
            ioe.printStackTrace();
            return;
        }

        while (true)
        {
            try { selector.select(); }
            catch (IOException ioe)
            {
                System.err.println("Error on select " + ioe.toString() + ioe.getMessage());
                ioe.printStackTrace();
                break;
            }

            //ottengo l'insieme delle chiavi pronte a operazioni di I/O
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while (iterator.hasNext())
            {
                SelectionKey key = iterator.next();

                // rimuove la chiave dal Selected Set, ma non dal registered Set
                try
                {
                    //sto accettando una nuova connessione
                    if(key.isAcceptable())
                    {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                        System.out.println("Connessione dal client " + client.getLocalAddress());
                    }
                    else if(key.isReadable()) //sto per leggere da una socket
                    {
                        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
                        Operation op_in;
                        opCode answerCode = opCode.OP_FAIL;

                        try
                        {
                            op_in = (Operation) Utils.recvObject(clientSocketChannel);
                            if(op_in == null)
                                throw new NullPointerException();
                        }
                        catch(ClassNotFoundException | NullPointerException e)
                        {
                            System.err.println("Error on reading Operation " + e.toString() + " " + e.getMessage());
                            e.printStackTrace();
                            //in caso di errore sovrascrivo il codice con OP_FAIL
                            answerCode = opCode.OP_FAIL;
                            key.attach(answerCode);
                            key.interestOps(SelectionKey.OP_WRITE);
                            continue;
                        }

                        String usr = op_in.getUsername();
                        String psw = op_in.getPassword();

                        System.out.println("OPERAZIONE: " + op_in.getCode());
                        switch (op_in.getCode())
                        {

                            case FILE_LIST:
                            {
                                /* mando all'utente la lista di file che può gestire,
                                   impostando l'esito di tale invio, che manderò successivamente
                                 */
                                try
                                {
                                    ArrayList<String> nameToSend = new ArrayList<>();
                                    for (String s: registeredUsers.getUser(usr, psw).getFiles())
                                    {
                                        /* costruisco l'opportuno pattern che il client leggerà
                                           nomefile_owner_numsezioni
                                         */

                                        nameToSend.add(s + "_" + userFiles.get(s).getNsections());
                                    }
                                    Utils.sendObject(clientSocketChannel, nameToSend);
                                    answerCode = opCode.OP_OK;
                                }
                                catch(IOException ioe)
                                { answerCode = opCode.ERR_FILE_LIST; }
                                break;
                            }

                            case LOGIN:
                            {
                                UserInfo userInfo = registeredUsers.getUser(usr,psw);
                                if(userInfo == null)
                                    answerCode = opCode.ERR_USER_UNKNOWN;
                                else if(!userInfo.getPassword().equals(psw))
                                    answerCode = opCode.ERR_WRONG_PASSWORD;
                                else if(userInfo.isOnline())
                                    answerCode = opCode.ERR_USER_ALREADY_LOGGED;
                                else if(registeredUsers.setStatus(usr,psw,1))
                                {
                                    /* creo la directory (solo se non esiste già)
                                       che conterrà tutti i file creati da questo utente */

                                    Files.createDirectories(Paths.get(Utils.SERVER_FILES_PATH + usr));

                                    answerCode = opCode.OP_OK;
                                }
                                else
                                    answerCode = opCode.OP_FAIL;
                                break;
                            }

                            case LOGOUT:
                            {
                                if(registeredUsers.setStatus(usr,psw,0))
                                {
                                    answerCode = opCode.OP_OK;

                                    //mando la risposta al client (con OP_OK)
                                    Utils.sendBytes(clientSocketChannel,answerCode.toString().getBytes());

                                    //sblocco l'eventuale sezione di un file in modifica dall'utente
                                    String editingFilename = registeredUsers.getUser(usr,psw).getEditingFilename();
                                    int editingSection = registeredUsers.getUser(usr,psw).getEditingSection();

                                    if(!editingFilename.equals(""))
                                        userFiles.get(editingFilename).unlockSection(editingSection - 1);

                                    //chiudo la socket
                                    clientSocketChannel.close();
                                    key.cancel();
                                    continue;
                                }
                                else
                                    answerCode = opCode.OP_FAIL;

                                break;
                            }

                            case CREATE:
                            {
                                String filename = op_in.getFilename();
                                String collectionFileName = filename + "_" + usr;
                                int nsections = op_in.getSection();

                                //controllo che non esista un file con lo stesso nome, creato dallo stesso utente
                                if(userFiles.containsKey(collectionFileName))
                                {
                                    answerCode = opCode.ERR_FILE_ALREADY_EXISTS;
                                }
                                else //se non esiste, aggiungo il file alla collezione gestita dal server
                                {
                                    //creo la directory che conterrà le sezioni del file 'filename'
                                    Files.createDirectories(Paths.get(getPath(usr,filename,0)));


                                    boolean err = false;

                                    //creo le sezioni del file
                                    for (int i = 1; i <= nsections; i++)
                                    {
                                        File sec = new File(getPath(usr,filename,i));
                                        if(!sec.createNewFile())
                                        {
                                            err = true;
                                            break;
                                        }
                                    }

                                    //creo la struttura dati contenente le info del file, da aggiungere alla collezione
                                    FileInfo fileInfo = new FileInfo(usr,nsections);
                                    userFiles.putIfAbsent(collectionFileName, fileInfo);

                                    //aggiungo il file alla lista di quelli gestibili dall'utente che l'ha creato
                                    registeredUsers.getUser(usr,psw).addFile(collectionFileName);
                                    answerCode = (err) ? opCode.OP_FAIL : opCode.OP_OK;
                                }
                                break;
                            }

                            case SHOW:
                            case EDIT:
                            {
                                String filename = op_in.getFilename();
                                String owner = op_in.getOwner();
                                String collectionFilename = filename + "_" + owner;
                                int section = op_in.getSection();

                                if(registeredUsers.getUser(usr,psw).canEdit(collectionFilename)) //utente con permessi
                                {
                                    if(!userFiles.get(collectionFilename).isLocked(section-1)) //sezione non lockata
                                    {
                                        if(op_in.getCode().equals(opCode.EDIT))
                                        {
                                            //lock sulla sezione
                                            userFiles.get(collectionFilename).lockSection(section-1);

                                            //salvo quale file l'utente sta modificando
                                            registeredUsers.getUser(usr,psw).setEditingFilename(collectionFilename);
                                            registeredUsers.getUser(usr,psw).setEditingSection(section);
                                        }
                                        answerCode = opCode.OP_OK;
                                    }
                                    else
                                        answerCode = opCode.SECTION_EDITING;

                                }
                                else
                                    answerCode = opCode.ERR_PERMISSION_DENIED;

                                break;
                            }

                            case END_EDIT:
                            {
                                String filename = op_in.getFilename();
                                String owner = op_in.getOwner();
                                String collectionFilename = filename + "_" + owner;
                                int section = op_in.getSection();

                                //tengo traccia del fatto che l'utente non sta più editando
                                registeredUsers.getUser(usr,psw).setEditingFilename("");

                                //ricevo il nuovo file dal client
                                try{ Utils.transferFromSection(clientSocketChannel,true); }
                                catch(IOException ioe)
                                {
                                    answerCode = opCode.OP_FAIL;
                                    break;
                                }

                                answerCode = opCode.OP_OK;
                                assert (userFiles.get(collectionFilename).isLocked(section-1));
                                userFiles.get(collectionFilename).unlockSection(section-1);

                                break;
                            }

                            case SECTION_SEND:
                            {
                                String filename = op_in.getFilename();
                                String owner = op_in.getOwner();
                                int section = op_in.getSection();

                                try
                                {
                                    Utils.transferToSection(clientSocketChannel,getPath(usr,filename,section).replaceFirst("./",""));
                                    answerCode = opCode.OP_OK;
                                }
                                catch(IOException ioe)
                                {
                                    answerCode = opCode.OP_FAIL;
                                    break;
                                }
                            }

                            default:
                                break;
                        }

                        key.attach(answerCode);
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                    else if (key.isWritable()) //sto per scrivere su una socket
                    {
                        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
                        opCode code = (opCode) key.attachment();

                        Utils.sendBytes(clientSocketChannel,code.toString().getBytes());
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