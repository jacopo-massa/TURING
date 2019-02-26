package Utils;

import GUI.MyFrame;
import GUI.ServerPanel;
import GUI.TuringPanel;
import GUI.frameCode;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

public class Utils
{

    public static String CLIENT_FILES_PATH = "./client_files/";
    public static String SERVER_FILES_PATH = "./turing_files/";

    public static String ADDRESS = "127.0.0.1";
    public static int REGISTRATION_PORT = 5001;
    public static int CLIENT_PORT = 5002;
    public static int INVITE_PORT = 5003;
    public static int MULTICAST_PORT = 5004;

    /* utility per la gestione di ricezione/invio oggetti e bytes su socket */

    private static void writeN(SocketChannel socket, ByteBuffer buffer, int n) throws IOException
    {
        int counter = 0;

        do
        { counter += socket.write(buffer); }
        while(counter < n);
    }

    private static void readN(SocketChannel socket, ByteBuffer buffer, int n) throws IOException
    {
        int counter = 0;
        do
        { counter += socket.read(buffer); }
        while(counter < n);
    }

    private static ByteBuffer serializeObject(Serializable serializable) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(serializable);
        oos.close();

        return ByteBuffer.wrap(baos.toByteArray());
    }

    public static Serializable deserializeObject(byte[] bytes) throws IOException, ClassNotFoundException
    {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        return (Serializable) ois.readObject();
    }

    public static void sendObject(SocketChannel socket, Serializable serializable) throws IOException
    {
        ByteBuffer wrap = serializeObject(serializable);

        int length = wrap.remaining();
        sendLength(socket,length);

        writeN(socket,wrap,length);
    }

    public static Serializable recvObject(SocketChannel socket) throws IOException, ClassNotFoundException
    {
        ByteBuffer dataByteBuffer;

        int length = recvLength(socket);

        if(length != 0)
        {
            dataByteBuffer = ByteBuffer.allocate(length);

            readN(socket,dataByteBuffer,length);

            Serializable ret = deserializeObject(dataByteBuffer.array());

            // clean up
            dataByteBuffer.clear();
            return ret;
        }
        else
            return null;
    }

    private static void sendLength(SocketChannel socket, int length) throws IOException
    {
        ByteBuffer dimBuffer = ByteBuffer.wrap(new byte[4]);

        dimBuffer.clear();
        dimBuffer.putInt(length);
        dimBuffer.flip();

        writeN(socket,dimBuffer,4);
    }

    private static int recvLength(SocketChannel socket) throws IOException
    {
        ByteBuffer lengthBuffer = ByteBuffer.wrap(new byte[4]);

        readN(socket,lengthBuffer,4);

        lengthBuffer.flip();
        int length = lengthBuffer.getInt(0);
        lengthBuffer.clear();

        return length;
    }

    public static void sendBytes(SocketChannel socket, byte[] bytes) throws IOException
    {
        sendLength(socket,bytes.length);

        ByteBuffer bytesBuffer;

        bytesBuffer = ByteBuffer.wrap(bytes);
        writeN(socket,bytesBuffer,bytes.length);

        bytesBuffer.clear();
    }

    public static byte[] recvBytes(SocketChannel socket) throws IOException
    {
        //leggo la dimensione del code che sto per ricevere
        ByteBuffer bytesBuffer;
        byte[] answerBytes;

        int dim = recvLength(socket);

        bytesBuffer = ByteBuffer.allocate(dim);
        readN(socket,bytesBuffer,dim);

        bytesBuffer.flip();

        answerBytes = new byte[bytesBuffer.remaining()];
        bytesBuffer.get(answerBytes);

        return answerBytes;

    }

    public static void transferToSection(SocketChannel socket, String filepath) throws IOException
    {

        File f;
        FileInputStream fis;
        FileChannel fc;
        int fileLength;

        try
        {
            f = new File(filepath);
            fis = new FileInputStream(f);
            fc = fis.getChannel();
            fileLength = (int) f.length();
        }
        catch (FileNotFoundException a)
        {
            System.out.println(filepath + " NON TROVATO");
            throw new IOException();
        }

        //elimino "Utils.Utils.*_FILES_PATH/<username>/" dal filepath
        String pathWithoutSource = filepath.split("/",3)[2];

        byte[] pathBytes = pathWithoutSource.getBytes();

        //mando il path
        sendBytes(socket,pathBytes);


        //mando lunghezza del file
        sendLength(socket, fileLength);

        if(fileLength != 0)
        {
            //mando il file
            int n = 0;
            do{ n += fc.transferTo(0, fileLength, socket); }
            while(n < fileLength);
        }

        fc.close();
        fis.close();
    }

    public static void transferFromSection(SocketChannel socket, String username, boolean isServer) throws IOException
    {
        //leggo il path
        byte[] pathBytes = recvBytes(socket);
        String filepath = new String(pathBytes);

        filepath = ((isServer) ? Utils.SERVER_FILES_PATH : Utils.CLIENT_FILES_PATH) + username + "/" + filepath;

        Files.createDirectories(Paths.get(filepath.substring(0,filepath.lastIndexOf("/"))));

        File f = new File(filepath);

        f.createNewFile(); //se il file già esiste lo sovrascrivo

        FileOutputStream fos = new FileOutputStream(f);
        FileChannel fc = fos.getChannel();

        //leggo lunghezza del file
        int fileLength = recvLength(socket);

        if(fileLength != 0)
        {
            //leggo il file
            int n = 0;
            do{ n += fc.transferFrom(socket,0, fileLength); }
            while(n < fileLength);
        }

        fc.close();
        fos.close();
    }

    /* --------------------------------- */

    public static String getPath(String username, String filename, int section, boolean isServer)
    {
        return ((isServer) ? SERVER_FILES_PATH : CLIENT_FILES_PATH) +
                username + "/" + filename +
                ((section == 0) ? "" : ("/" + filename + section + ".txt"));
    }

    public static void deleteDirectory(String path) throws IOException
    {
        Files.walk(Paths.get(path))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public static void showPreviousFrame(Component c)
    {
        //ottengo il frame corrente
        MyFrame currentFrame = (MyFrame) SwingUtilities.getWindowAncestor(c);

        //ottengo il suo precedente
        MyFrame oldFrame = currentFrame.getOldFrame();

        oldFrame.setVisible(true);
        currentFrame.dispose();
    }

    public static void showNextFrame(frameCode frame, Component c)
    {
        //ottengo il frame corrente
        MyFrame oldFrame = (MyFrame) SwingUtilities.getWindowAncestor(c);

        //mostro il frame successivo
        new MyFrame(oldFrame, frame);
    }

    public static synchronized void appendToPane(JTextPane tp, String msg, Color c, boolean bold)
    {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

        if(bold)
            aset = sc.addAttribute(aset, StyleConstants.Bold, Boolean.TRUE);

        int len = tp.getDocument().getLength();
        tp.setCaretPosition(len);
        tp.setCharacterAttributes(aset, false);

        Document doc = tp.getStyledDocument();
        try{ doc.insertString(doc.getLength(),msg,aset);}
        catch (BadLocationException e){e.printStackTrace();}
    }

    public static void printTimeStamp(JTextPane pane, Date date)
    {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        String textToAppend = "[" + formatter.format(date) + "] - ";

        Utils.appendToPane(pane, textToAppend, Color.BLACK,false);
    }

    public static void printInvite(String owner, String filename, Date date)
    {
        printTimeStamp(TuringPanel.receiveArea, date);

        String textToAppend = "invite from ";
        Utils.appendToPane(TuringPanel.receiveArea, textToAppend, Color.BLUE,false);

        Utils.appendToPane(TuringPanel.receiveArea, owner, Color.RED,true);

        textToAppend = " for document ";
        Utils.appendToPane(TuringPanel.receiveArea, textToAppend, Color.BLUE,false);

        Utils.appendToPane(TuringPanel.receiveArea, filename + "\n", Color.RED,true);
    }

    public static void printLog(String msg, opCode requestCode, opCode answerCode)
    {
        //printTimeStamp(ServerPanel.logPane, new Date());

        Utils.appendToPane(ServerPanel.logPane, msg, Color.RED,true);
        Utils.appendToPane(ServerPanel.logPane, "  REQUEST: ", Color.BLACK,false);
        Utils.appendToPane(ServerPanel.logPane, String.valueOf(requestCode), Color.BLUE,true);
        Utils.appendToPane(ServerPanel.logPane, "  RESULT: ", Color.BLACK,false);

        Color c = (answerCode == opCode.OP_FAIL) ? Color.RED : Color.GREEN;
        Utils.appendToPane(ServerPanel.logPane, answerCode + "\n", c,true);
    }

    public static void sendChatMessage(String sender, String msg, String address, Component component)
    {
        if(address == null)
        {
            if(component != null)
                JOptionPane.showMessageDialog(component,"Non stai editando nessun file!","WARNING",JOptionPane.WARNING_MESSAGE);
        }
        else if (!msg.trim().isEmpty())
        {
            DatagramSocket socket;
            InetAddress group;

            try
            {
                socket = new DatagramSocket();
                group = InetAddress.getByName(address);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                if(component != null)
                    JOptionPane.showMessageDialog(component,"Errore nell'invio del messaggio","ERRORE",JOptionPane.ERROR_MESSAGE);
                return;
            }

            Message message = new Message(sender, msg, new Date());

            try
            {
                ByteBuffer msgBuffer = Utils.serializeObject(message);
                byte[] msgBytes = msgBuffer.array();

                byte[] lengthBytes = Integer.toString(msgBytes.length).getBytes();
                DatagramPacket lengthPacket = new DatagramPacket(lengthBytes, lengthBytes.length, group, Utils.MULTICAST_PORT);
                socket.send(lengthPacket);

                DatagramPacket msgPacket = new DatagramPacket(msgBytes, msgBytes.length, group, Utils.MULTICAST_PORT);
                socket.send(msgPacket);
            }
            catch(IOException e)
            {
                System.err.println("Can't serialize chat Utils.Message");
                if(component != null)
                    JOptionPane.showMessageDialog(component,"Errore nell'invio del messaggio","ERRORE",JOptionPane.ERROR_MESSAGE);
                return;
            }

            socket.close();
        }
    }
}
