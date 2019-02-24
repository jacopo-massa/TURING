import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

class Utils
{

    static String CLIENT_FILES_PATH = "./client_files/";
    static String SERVER_FILES_PATH = "./turing_files/";

    static String ADDRESS = "127.0.0.1";
    static int REGISTRATION_PORT = 5000;
    static int CLIENT_PORT = 5001;
    static int INVITE_PORT = 5002;

    static void writeN(SocketChannel socket, ByteBuffer buffer, int n) throws IOException
    {
        int counter = 0;

        do
        { counter += socket.write(buffer); }
        while(counter < n);
    }

    static void readN(SocketChannel socket, ByteBuffer buffer, int n) throws IOException
    {
        int counter = 0;
        do
        { counter += socket.read(buffer); }
        while(counter < n);
    }

    static void sendObject(SocketChannel socket, Serializable serializable) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for(int i=0;i<4;i++)
            baos.write(0);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(serializable);
        oos.close();
        ByteBuffer wrap = ByteBuffer.wrap(baos.toByteArray());
        wrap.putInt(0, baos.size()-4);
        //socket.write(wrap);
        writeN(socket,wrap,baos.size());
    }

    static Serializable recvObject(SocketChannel socket) throws IOException, ClassNotFoundException
    {
        ByteBuffer lengthByteBuffer = ByteBuffer.wrap(new byte[4]);
        ByteBuffer dataByteBuffer = null;
        int length = 0;

        readN(socket,lengthByteBuffer,4);
        if (lengthByteBuffer.remaining() == 0)
        {
            length = lengthByteBuffer.getInt(0);
            dataByteBuffer = ByteBuffer.allocate(length);
            lengthByteBuffer.clear();
        }

        readN(socket,dataByteBuffer,length);

        if(dataByteBuffer == null)
            return null;

        if (dataByteBuffer.remaining() == 0)
        {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(dataByteBuffer.array()));
            final Serializable ret = (Serializable) ois.readObject();
            // clean up
            dataByteBuffer.clear();
            return ret;
        }
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

    static void sendBytes(SocketChannel socket, byte[] bytes) throws IOException
    {
        sendLength(socket,bytes.length);

        ByteBuffer bytesBuffer;

        bytesBuffer = ByteBuffer.wrap(bytes);
        writeN(socket,bytesBuffer,bytes.length);

        bytesBuffer.clear();
    }

    static byte[] recvBytes(SocketChannel socket) throws IOException
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

    static void transferToSection(SocketChannel socket, String filepath) throws IOException
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

        //elimino Utils.*_FILES_PATH dal filepath
        String pathWithoutSource = filepath.split("/",2)[1];

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

    static void transferFromSection(SocketChannel socket, boolean isServer) throws IOException
    {
        //leggo il path
        byte[] pathBytes = recvBytes(socket);
        String filepath = new String(pathBytes);

        filepath = ((isServer) ? Utils.SERVER_FILES_PATH : Utils.CLIENT_FILES_PATH) + filepath;

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

    static void deleteDirectory(String path) throws IOException
    {
        Files.walk(Paths.get(path))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    static void showNextFrame(frameCode frame, Component c)
    {
        //nascondo il frame corrente
        MyFrame old_f = (MyFrame) SwingUtilities.getWindowAncestor(c);
        Point oldLocation = old_f.getLocationOnScreen();
        old_f.setVisible(false);

        //mostro il frame successivo
        new MyFrame(oldLocation, frame);
    }

    static synchronized void appendToPane(JTextPane tp, String msg, Color c, boolean bold)
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

    static void printInvite(String owner, String filename, Date date)
    {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        String textToAppend = "[" + formatter.format(date) + "] - ";

        Utils.appendToPane(TuringPanel.receiveArea, textToAppend, Color.BLACK,false);

        textToAppend = "invite from ";
        Utils.appendToPane(TuringPanel.receiveArea, textToAppend, Color.BLUE,false);

        Utils.appendToPane(TuringPanel.receiveArea, owner, Color.RED,true);

        textToAppend = " for document ";
        Utils.appendToPane(TuringPanel.receiveArea, textToAppend, Color.BLUE,false);

        Utils.appendToPane(TuringPanel.receiveArea, filename + "\n", Color.RED,true);
    }
}
