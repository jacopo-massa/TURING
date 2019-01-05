import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;

public class MainServer
{
    public static int REGISTRATION_PORT = 5000;
    public static int PORT = 5001;
    private static String SERVER_USER = "server";

    public static void main(String[] args)
    {
        RegisteredUsers registeredUsers = new RegisteredUsers();
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
                            //mantengo il codice della richiesta che mi è stata fatta
                            op_out.setCode(op_in.getCode());
                        }
                        catch(ClassNotFoundException | NullPointerException e)
                        {
                            System.out.println("IN READ:" + op_out.getCode());
                            System.err.println("Error on reading Operation" + e.toString() + e.getMessage());
                            e.printStackTrace();
                            //in caso di errore sovrascrivo il codice con OP_FAIL
                            op_out.setCode(opCode.OP_FAIL);
                            key.attach(op_out);
                            key.interestOps(SelectionKey.OP_WRITE);
                            continue;
                        }

                        String usr = op_in.getUsername();
                        String psw = op_in.getPassword();

                        switch (op_in.getCode())
                        {
                            case LOGIN:
                            {
                                User user = registeredUsers.getUser(usr,psw);
                                if(user == null || !user.getPassword().equals(psw))
                                    op_out.setCode(opCode.OP_FAIL);
                                else if(registeredUsers.setStatus(usr,psw,1))
                                    op_out.setCode(opCode.OP_OK);
                                else
                                    op_out.setCode(opCode.OP_FAIL);

                                break;
                            }

                            case LOGOUT:
                            {
                                User user = registeredUsers.getUser(usr,psw);
                                if(user == null || !user.getPassword().equals(psw))
                                    op_out.setCode(opCode.OP_FAIL);

                                else if(registeredUsers.setStatus(usr,psw,0))
                                    op_out.setCode(opCode.OP_OK);
                                else
                                    op_out.setCode(opCode.OP_FAIL);


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
