import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.*;
import java.nio.file.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class MainServer
{
    public static int REGISTRATION_PORT = 5000;
    public static int PORT = 5001;
    private static String SERVER_USER = "server";
    private static String USER_FILES_PATH = "./turing_files/";

    private static String getPath(String username, String filename, int section)
    {
        return USER_FILES_PATH + username + "/" + filename + ((section == 0) ? "" : ("/" + filename + "_" + section));
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
                new Thread(new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            Files.walk(Paths.get(USER_FILES_PATH))
                                    .sorted(Comparator.reverseOrder())
                                    .map(Path::toFile)
                                    .forEach(File::delete);
                        }
                        catch(IOException ioe) {}

                    }
                }));

        /* Creazione del socket TCP per tutte le altre richieste dei client */

        ServerSocketChannel serverSocketChannel;
        Selector selector;
        try
        {
            serverSocketChannel = ServerSocketChannel.open();
            ServerSocket serverSocket = serverSocketChannel.socket();
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
                        Operation op_out = new Operation(SERVER_USER);

                        try
                        {
                            op_in = (Operation) Utils.recvObject(clientSocketChannel);
                            if(op_in == null)
                                throw new NullPointerException();
                        }
                        catch(ClassNotFoundException | NullPointerException e)
                        {
                            System.out.println("IN READ:" + op_out.getCode());
                            System.err.println("Error on reading Operation " + e.toString() + " " + e.getMessage());
                            e.printStackTrace();
                            //in caso di errore sovrascrivo il codice con OP_FAIL
                            op_out.setCode(opCode.OP_FAIL);
                            key.attach(op_out);
                            key.interestOps(SelectionKey.OP_WRITE);
                            continue;
                        }

                        String usr = op_in.getUsername();
                        String psw = op_in.getPassword();

                        System.out.println("OPERAZIONE: " + op_in.getCode());
                        switch (op_in.getCode())
                        {
                            case LOGIN:
                            {
                                UserInfo userInfo = registeredUsers.getUser(usr,psw);
                                if(userInfo == null)
                                    op_out.setCode(opCode.ERR_USER_UNKNOWN);
                                else if(!userInfo.getPassword().equals(psw))
                                    op_out.setCode(opCode.ERR_WRONG_PASSWORD);
                                else if(userInfo.isOnline())
                                    op_out.setCode(opCode.ERR_USER_ALREADY_LOGGED);
                                else if(registeredUsers.setStatus(usr,psw,1))
                                {
                                    op_out.setCode(opCode.OP_OK);

                                    /* creo la directory che conterrà tutti i file creati da questo utente */
                                    Files.createDirectories(Paths.get(USER_FILES_PATH + usr));
                                }
                                else
                                    op_out.setCode(opCode.OP_FAIL);
                                break;
                            }

                            case LOGOUT:
                            {
                                UserInfo userInfo = registeredUsers.getUser(usr,psw);
                                if(registeredUsers.setStatus(usr,psw,0))
                                {
                                    op_out.setCode(opCode.OP_OK);
                                    //mando la risposta al client (con OP_OK)
                                    Utils.sendObject(clientSocketChannel,op_out);
                                    //chiudo la socket
                                    clientSocketChannel.close();
                                    key.cancel();
                                    continue;
                                }
                                else
                                    op_out.setCode(opCode.OP_FAIL);
                            }

                            case CREATE:
                            {
                                String filename = op_in.getFilename();
                                String collectionFileName = filename + "_" + usr;
                                int nsections = op_in.getSection();

                                //controllo che non esista un file con lo stesso nome, creato dallo stesso utente
                                if(userFiles.containsKey(collectionFileName))
                                {
                                    op_out.setCode(opCode.ERR_FILE_ALREADY_EXISTS);
                                }
                                else //se non esiste ,aggiungo il file alla collezione gestita dal server
                                {
                                    //creo la directory che conterrà le sezioni del file 'filename'
                                    Files.createDirectories(Paths.get(getPath(usr,filename,0)));

                                    //creo le sezioni del file
                                    for (int i = 1; i <= nsections; i++)
                                    {
                                        File sec = new File(getPath(usr,filename,i) + ".txt");
                                        if(!sec.createNewFile())
                                        {
                                            op_out.setCode(opCode.OP_FAIL);
                                            break;
                                        }
                                    }

                                    //creo la struttura dati contenente le info del file, da aggiungere alla collezione
                                    FileInfo fileInfo = new FileInfo(usr,nsections);
                                    userFiles.putIfAbsent(collectionFileName, fileInfo);
                                    op_out.setCode(opCode.OP_OK);
                                }
                                break;
                            }

                            case OP_FAIL:
                            default:
                                op_out.setCode(opCode.OP_FAIL);
                                break;
                        }

                        key.attach(op_out);
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                    else if (key.isWritable()) //sto per scrivere su una socket
                    {
                        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
                        Operation op = (Operation) key.attachment();

                        Utils.sendObject(clientSocketChannel,op);
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
