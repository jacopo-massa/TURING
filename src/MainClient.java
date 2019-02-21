import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;


public class MainClient
{
    public static int REGISTRATION_PORT = 5000;
    public static int PORT = 5001;

    static InetSocketAddress address;
    static SocketChannel clientSocketChannel;

    static String username;
    static String password;

    public static void main(String[] args)
    {
        new MyFrame("LOGIN");
    }

    public static boolean sendReq(Operation op)
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

    public static opCode getAnswer()
    {
        try
        {
            opCode answer = Utils.recvOpCode(clientSocketChannel);
            if(answer == null)
            {
                System.out.println("CHIUSO");
                clientSocketChannel.close();
                return opCode.OP_FAIL;
            }
            else
                return answer;

        }
        catch (IOException | NullPointerException e)
        {
            System.err.println("Error in reading object");
            e.printStackTrace();
            return null;
        }
    }

    public static int register()
    {
        IntRegistration registration;
        Remote remote;

        try
        {
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

    public static opCode loginUser()
    {
        address = new InetSocketAddress("127.0.0.1",PORT);

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

    public static opCode logoutUser()
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

    public static opCode manageDocument(opCode code, String name, String owner, int nsection)
    {
        Operation request = new Operation(username);
        request.setPassword(password);
        if(nsection != 0)
        {
            request.setCode(code);
            request.setSection(nsection);
        }
        else
            request.setCode(opCode.SHOW_ALL);

        request.setFilename(name);
        request.setOwner(owner);
        sendReq(request);

        return getAnswer();
    }
}
