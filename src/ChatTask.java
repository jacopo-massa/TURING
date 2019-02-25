import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatTask implements Runnable
{
    private String address;
    private int port;
    private String username;


    public ChatTask(String address, int port)
    {
        this.username = LoginPanel.usr;
        this.address = address;
        this.port = port;
    }

    public void run()
    {
        MulticastSocket socket;
        InetAddress group;
        try
        {
            socket = new MulticastSocket(port);
            group = InetAddress.getByName(address);
            socket.joinGroup(group);
        }
        catch(IOException e)
        {
            System.err.println("Error opening Multicast Socket");
            return;
        }

        byte[] lengthBytes = new byte[4];
        byte [] msgBytes;

        DatagramPacket lengthPacket;
        DatagramPacket msgPacket;

        int length;
        Message message;

        while(true)
        {
            lengthPacket = new DatagramPacket(lengthBytes,4);
            try
            {
                socket.receive(lengthPacket);
            }
            catch (IOException e)
            {
                System.err.println("Error receiving chat msg length");
                continue;
            }

            length = Integer.parseInt(new String(lengthPacket.getData(),lengthPacket.getOffset(),lengthPacket.getLength()));

            msgBytes = new byte[length];
            msgPacket = new DatagramPacket(msgBytes, msgBytes.length);

            try
            {
                socket.receive(msgPacket);
                message = (Message) Utils.deserializeObject(msgBytes);

                boolean isMe = (username.equals(message.getSender()));

                //controllo se ho ricevuto il messaggio di terminazione
                if(message.getBody().equals("left the chat"))
                {
                    if(isMe)
                    {
                        message.setSender("me");
                        TuringPanel.receiveArea.setText("");
                        printChatMessage(message);
                        socket.leaveGroup(group);
                        break;
                    }
                    else
                        printChatMessage(message);
                }
                else
                    printChatMessage(message);
            }
            catch (ClassNotFoundException | IOException e)
            { System.err.println("Error receiving chat msg"); }
        }
        socket.close();
    }

    private void printChatMessage(Message message)
    {
        Utils.printTimeStamp(message.getDate());

        String textToAppend = message.getSender() + ": ";
        Utils.appendToPane(TuringPanel.receiveArea, textToAppend, Color.RED,true);

        Utils.appendToPane(TuringPanel.receiveArea, message.getBody() + "\n", Color.BLUE,true);
    }
}
