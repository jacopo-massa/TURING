import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class MainClient
{
        static SocketChannel clientSocketChannel;

    static String username;
    static String password;

    public static void main(String[] args)
    {
        new MyFrame(frameCode.LOGIN);

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    try { Utils.deleteDirectory(Utils.CLIENT_FILES_PATH + username); }
                    catch(IOException ioe) {System.err.println("Can't delete " + Utils.CLIENT_FILES_PATH + username); }
                }));
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
            System.err.println("Error in reading object");
            e.printStackTrace();
            return opCode.OP_FAIL;
        }
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
        int PORT = 5001;
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", PORT);

        try
        {
            clientSocketChannel = SocketChannel.open();
            clientSocketChannel.connect(address);

            //aspetto che termini la connessione
            while (!clientSocketChannel.finishConnect())
            {
                System.out.println("Non terminata la connessione");
            }
        }
        catch(IOException ioe)
        {
            System.err.println("Error in connecting to server");
            ioe.printStackTrace();
            return opCode.OP_FAIL;
        }

        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(opCode.LOGIN);
        sendReq(request);

        return getAnswer();
    }

    static opCode logoutUser()
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        request.setCode(opCode.LOGOUT);
        sendReq(request);
        opCode answer = getAnswer();
        if(answer == opCode.OP_FAIL)
        {
            try { clientSocketChannel.close(); }
            catch(IOException ioe)
            {
                System.err.println("Error in closing clientSocketChannel");
                ioe.printStackTrace();
            }
        }

        return answer;
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

        opCode answerCode = getAnswer();


        /* TODO - aggiustare condizioni per SHOW e EDIT */
        if(answerCode == opCode.OP_OK)
        {
            boolean showAll = (code == opCode.SHOW_ALL);

            for (int i = (showAll) ? 1 : nsection; i <= nsection; i++)
            {
                try
                {

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
}
