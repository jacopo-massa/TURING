import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;


public class MainClient
{
    static SocketChannel clientSocketChannel;
    static SocketChannel inviteSocketChannel;
    static ArrayList<Boolean> editedSections;
    static Thread inviteThread;

    static String username;
    static String password;
    static ArrayList<Invitation> pendingInvitations;

    public static void main(String[] args)
    {
        //creo il frame di login
        new MyFrame(null,frameCode.LOGIN);

        //imposto una funzione di cleanup che elimina i file dell'utente alla terminazione del client
        Runtime.getRuntime().addShutdownHook(
                new Thread(() ->
                {
                    try
                    {
                        Utils.deleteDirectory(Utils.CLIENT_FILES_PATH + username);

                    }
                    catch(IOException ioe) {System.err.println("Can't delete " + Utils.CLIENT_FILES_PATH + username); }
                    logoutUser();
                }));

        //creo un thread che resta in ascolto per gli inviti in tempo reale
    }

    static boolean sendReq(Operation op)
    {
        try
        {
            Utils.sendObject(clientSocketChannel,op);
        }
        catch(IOException ioe)
        {
            System.err.println("Error in sending operation " + op.getCode());
            ioe.printStackTrace();
            return false;
        }

        return true;
    }

    static opCode getAnswer()
    {
        try
        {
            byte[] answerBytes = Utils.recvBytes(clientSocketChannel);

            if(answerBytes.length == 0)
            {
                System.out.println("CHIUSO");
                clientSocketChannel.close();
                return opCode.OP_FAIL;
            }
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

    static SocketChannel openConnection(int port)
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

    static int register()
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
            return -1;
        }

        try
        {
            if(registration.registerUser(username,password))
            {
                loginUser();
                return 1;
            }
            else
                return 0;
        }
        catch(RemoteException re)
        {
            System.err.println("Error in invoking method \"registerUser\" ");
            re.printStackTrace();
            return -1;
        }
    }

    static opCode loginUser()
    {
        opCode answerCode;

        clientSocketChannel = openConnection(Utils.CLIENT_PORT);

        if(clientSocketChannel == null)
            answerCode = opCode.OP_FAIL;
        else
        {
            Operation request = new Operation(username);
            request.setPassword(password);
            request.setCode(opCode.LOGIN);
            sendReq(request);

            inviteSocketChannel = openConnection(Utils.INVITE_PORT);
            if(inviteSocketChannel == null)
            {
                try {clientSocketChannel.close();}
                catch (IOException e) {e.printStackTrace();}
                answerCode = opCode.OP_FAIL;
            }
            else
            {
                answerCode = getAnswer();

                if(answerCode == opCode.OP_OK)
                {
                    //creo il thread per la ricezione degli inviti
                    inviteThread = new Thread(new InviteTask(inviteSocketChannel));
                    inviteThread.start();

                    //ottengo la lista degli inviti ricevuti quando sia era offline

                    request.setCode(opCode.PENDING_INVITATIONS);
                    sendReq(request);

                    try
                    {
                        pendingInvitations = (ArrayList<Invitation>) Utils.recvObject(clientSocketChannel);

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

    static opCode logoutUser()
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(opCode.LOGOUT);
        sendReq(request);

        //chiudo socket del client
        try { clientSocketChannel.close(); }
        catch(IOException ioe)
        {
            System.err.println("Error in closing clientSocketChannel");
            ioe.printStackTrace();
        }

        //interrompo il thread degli inviti
        inviteThread.interrupt();

        //chiudo socket degli inviti
        try { inviteSocketChannel.close(); }
        catch(IOException ioe)
        {
            System.err.println("Error in closing inviteSocketChannel");
            ioe.printStackTrace();
        }

        return opCode.OP_OK;
    }

    static opCode createDocument(String name, int nsection)
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(opCode.CREATE);

        request.setFilename(name);
        request.setSection(nsection);

        sendReq(request);

        return getAnswer();
    }

    static opCode manageDocument(opCode code, String name, String owner, int nsection)
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(code);
        request.setFilename(name);
        request.setOwner(owner);
        request.setSection(nsection);

        sendReq(request);


        opCode answerCode;
        boolean showAll = (code == opCode.SHOW_ALL);

        if(showAll)
        {
            try
            {
                editedSections = (ArrayList<Boolean>) Utils.recvObject(clientSocketChannel);
                answerCode = getAnswer();
            }
            catch (ClassNotFoundException | IOException e)
            {
                System.err.println("Can't download list of edited sections");
                answerCode = opCode.OP_FAIL;
            }
        }
        else
            answerCode = getAnswer();

        if(answerCode == opCode.OP_OK || (code != opCode.EDIT && answerCode == opCode.SECTION_EDITING))
        {
            for (int i = (showAll) ? 1 : nsection; i <= nsection; i++)
            {
                try
                {
                    request.setCode(opCode.SECTION_RECEIVE);
                    request.setSection(i);
                    sendReq(request);

                    Utils.transferFromSection(clientSocketChannel,false);
                    answerCode = getAnswer();
                }
                catch(IOException ioe)
                { answerCode = opCode.OP_FAIL; }
            }
        }
        return answerCode;
    }

    static opCode endEditDocument(String name, String owner, int section)
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
            String filepath = Utils.CLIENT_FILES_PATH + username + "/" + name + "/" + name + section + ".txt";
            Utils.transferToSection(clientSocketChannel,filepath.replaceFirst("./",""));
            answerCode = getAnswer();
        }
        catch(IOException ioe)
        { answerCode = opCode.OP_FAIL; }

        return answerCode;
    }

    static opCode invite(String filename, String collaborator)
    {
        Operation request = new Operation(collaborator);
        request.setPassword(password);
        request.setCode(opCode.INVITE);
        request.setFilename(filename);
        request.setOwner(username);

        sendReq(request);
        opCode answerCode = getAnswer();
        return answerCode;
    }

}
