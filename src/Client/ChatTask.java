package Client;

import GUI.TuringPanel;
import Utils.Message;
import Utils.Utils;

import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.channels.ClosedChannelException;

public class ChatTask implements Runnable
{
    private String address;
    private int port;
    private String username;

    /**
     * ChatTask è il target eseguito dal thread della chat, istanziato in MainClient.
     *
     * @param address indirizzo su cui aprire la MulticastSocket
     * @param port numero di porta sui cui aprire la MulticastSocket
     */
    ChatTask(String address, int port)
    {
        this.username = MainClient.username;
        this.address = address;
        this.port = port;
    }

    /**
     * metodo eseguito dal thread chat quando viene fatto partire.
     */
    public void run()
    {
        MulticastSocket socket;
        InetAddress group;
        Message message = null;

        DatagramPacket lengthPacket;
        DatagramPacket msgPacket;

        try
        {
            //effettuo il bind della socket sulla porta 'port'
            socket = new MulticastSocket(port);
            group = InetAddress.getByName(address);

            //unisco il client al gruppo all'indirizzo 'address'
            System.out.println(System.getProperty("java.net.preferIPv4Stack"));
            socket.joinGroup(group);
        }
        catch(IOException e)
        {
            e.printStackTrace();
            System.err.println("Error opening Multicast Socket");
            return;
        }

        byte[] lengthBytes = new byte[4];
        byte[] msgBytes;
        int length;

        while(!Thread.interrupted())
        {

            lengthPacket = new DatagramPacket(lengthBytes,4);

            // leggo la lunghezza dell'oggetto 'Message' che sto per ricevere
            try
            { socket.receive(lengthPacket); }
            catch(ClosedChannelException e)
            {
                System.err.println("Chat interrupted");
                break;
            }
            catch (IOException e)
            {
                System.err.println("Error receiving chat msg length");
                continue;
            }

            length = Integer.parseInt(new String(lengthPacket.getData(),lengthPacket.getOffset(),lengthPacket.getLength()));

            msgBytes = new byte[length];
            msgPacket = new DatagramPacket(msgBytes, msgBytes.length);

            // leggo il messaggio ricevuto
            try
            {
                socket.receive(msgPacket);
                message = (Message) Utils.deserializeObject(msgBytes);

                boolean isMe = (username.equals(message.getSender()));

                if(isMe)
                    message.setSender("me");

                // controllo se sto entrando in una nuova chat
                if(message.getBody().equals("joined the chat") && isMe)
                    TuringPanel.receiveArea.setText("");

                // controllo se sto uscendo da una chat
                if(message.getBody().equals("left the chat") && isMe)
                {
                    TuringPanel.receiveArea.setText("");
                    break;
                }

                // stampo il messaggio ricevuto
                printChatMessage(message);
            }
            catch(ClosedChannelException e)
            {
                System.err.println("Chat interrupted for ClosedChannel");
                break;
            }
            catch (ClassNotFoundException | IOException e)
            { System.err.println("Error receiving chat msg"); }
        }

        // prima di terminare il thread, lascio il gruppo a cui mi ero unito e chiudo la socket
        try
        {
            socket.leaveGroup(group);
            socket.close();
            if(message != null)
                printChatMessage(message);
        }
        catch (IOException e)
        {
            System.err.println("Error closing Multicast Socket");
        }

        System.err.println("Chat interrupted");
    }

    /**
     * Procedura che formatta il messaggio ricevuto e lo stampa sul pannello contenuto in TuringPanel.
     *
     * @param message messaggio da stampare
     */
    private void printChatMessage(Message message)
    {
        Utils.printTimeStamp(TuringPanel.receiveArea, message.getDate());

        String textToAppend = message.getSender() + ": ";

        Utils.appendToPane(TuringPanel.receiveArea, textToAppend, Color.RED,true);

        String body = message.getBody();

        if(body.equals("left the chat") || body.equals("joined the chat"))
            Utils.appendToPane(TuringPanel.receiveArea, body + "\n", Color.GREEN,true);
        else
            Utils.appendToPane(TuringPanel.receiveArea, body + "\n", Color.BLUE,false);
    }
}
