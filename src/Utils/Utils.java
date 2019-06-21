package Utils;

import GUI.FrameCode;
import GUI.MyFrame;
import GUI.ServerPanel;
import GUI.TuringPanel;

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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

/**
 * Classe contenente costanti e funzioni di utilità, utilizzate in tutto il progetto.
 */
public class Utils
{
    // costanti che indicano i path delle home directory dei file nei client / server TURING
    public static String CLIENT_FILES_PATH = "./client_files/";
    public static String SERVER_FILES_PATH = "./turing_files/";

    /* costanti che indicano l'indirizzo e le porte su cui aprire le socket TCP/UDP per la
       comunicazione tra clients e server
      */
    public static String ADDRESS = "127.0.0.1";
    public static int REGISTRATION_PORT = 5001;
    public static int CLIENT_PORT = 5002;
    public static int MULTICAST_PORT = 5003;

    /* Procedure che assicurano la scrittura di esattamente 'n' bytes sul socket 'socket' */

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


    /* Utility per la gestione di ricezione / invio di Objects (serializzabili) e bytes su socket */

    /**
     * Procedura per scrivere un intero sul socket (sempre la lunghezza di un oggetto scritto successivamente).
     *
     * @param socket socket su cui scrivere l'intero
     * @param length intero da scrivere
     *
     * @throws IOException errore nella scrittura sul socket
     */
    private static void sendLength(SocketChannel socket, int length) throws IOException
    {
        ByteBuffer dimBuffer = ByteBuffer.wrap(new byte[4]);

        dimBuffer.clear();
        dimBuffer.putInt(length);
        dimBuffer.flip();

        writeN(socket,dimBuffer,4);
    }

    /**
     * Funzione  per leggere un intero dal socket (sempre la lunghezza di un oggetto letto successivamente).
     *
     * @param socket sokcet da cui leggere l'intero
     *
     * @return intero letto
     *
     * @throws IOException errore nella lettura sul socket
     */
    private static int recvLength(SocketChannel socket) throws IOException
    {
        ByteBuffer lengthBuffer = ByteBuffer.wrap(new byte[4]);

        readN(socket,lengthBuffer,4);

        lengthBuffer.flip();
        int length = lengthBuffer.getInt(0);
        lengthBuffer.clear();

        return length;
    }

    /**
     * Funzione per trasformare un oggetto in un array di byte
     * @param serializable oggetto da serializzare
     * @return ByteBuffer contenente l'oggetto serializzato
     * @throws IOException errore nell' apertura o scrittura sullo stream
     */
    private static ByteBuffer serializeObject(Serializable serializable) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(serializable);
        oos.close();

        return ByteBuffer.wrap(baos.toByteArray());
    }

    /**
     * Funzione per convertire un array di byte in un oggetto.
     *
     * @param bytes array di bytes da cui estrarre l'oggetto
     *
     * @return l'oggetto.
     * @throws IOException errore nell' apertura o lettura dallo stream
     * @throws ClassNotFoundException la classe dell'oggetto serializzato non è stata trovata
     */
    public static Serializable deserializeObject(byte[] bytes) throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (Serializable) ois.readObject();
    }

    /**
     * Procedura per mandare un oggetto (opportunamente trasformato in un array di byte) su un socket.
     *
     * @param socket socket su cui mandare l'oggetto
     * @param serializable oggetto da mandare
     *
     * @throws IOException errore di I/O sul socket o stream
     */
    public static void sendObject(SocketChannel socket, Serializable serializable) throws IOException
    {
        ByteBuffer wrap = serializeObject(serializable);

        int length = wrap.remaining();

        // mando la lunghezza dell'oggetto serializzato
        sendLength(socket,length);

        if(length != 0)
            // mando l'oggetto serializzato
            writeN(socket,wrap,length);
    }

    /**
     * Funzione per leggere un oggetto da un socket.
     *
     * @param socket socket da cui leggere l'oggetto
     * @return oggetto, se la lettura è avvenuta correttamente
     *         null altrimenti
     *
     * @throws IOException errore di I/O su socket o stream
     * @throws ClassNotFoundException la classe dell'oggetto serializzato non è stata trovata
     */
    public static Serializable recvObject(SocketChannel socket) throws IOException, ClassNotFoundException
    {
        ByteBuffer dataByteBuffer;

        // ricevo la lunghezza dell'oggetto serializzato
        int length = recvLength(socket);

        if(length != 0)
        {
            dataByteBuffer = ByteBuffer.allocate(length);

            // ricevo l'oggetto serializzato
            readN(socket,dataByteBuffer,length);
            Serializable ret = deserializeObject(dataByteBuffer.array());

            // clean up
            dataByteBuffer.clear();
            return ret;
        }
        else
            return null;
    }

    /**
     * Procedura per spedire un array di bytes su un socket.
     *
     * @param socket socket su cui spedire l'array di bytes
     * @param bytes array di bytes da spedire
     *
     * @throws IOException errore di scrittura sul socket
     */
    public static void sendBytes(SocketChannel socket, byte[] bytes) throws IOException
    {
        sendLength(socket,bytes.length);

        ByteBuffer bytesBuffer;

        bytesBuffer = ByteBuffer.wrap(bytes);
        writeN(socket,bytesBuffer,bytes.length);

        bytesBuffer.clear();
    }

    /**
     * Funzione per leggere un array di bytes da un socket.
     *
     * @param socket socket da cui leggere l'array di bytes.
     * @return array di bytes letto.
     *
     * @throws IOException errore di lettura dal socket
     */
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

    /**
     * Procedura che scrive una sezione di un documento su un socket.
     *
     * @param socket socket su cui scrivere
     * @param filepath path della sezione da scrivere
     *
     * @throws IOException errore di I/O sul socket o filepath errato
     */
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

        // elimino "Utils.Utils.*_FILES_PATH/<username>/" dal filepath
        String pathWithoutSource = filepath.split("/",3)[2];

        byte[] pathBytes = pathWithoutSource.getBytes();

        // mando il path
        sendBytes(socket,pathBytes);


        // mando lunghezza del file
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

    /**
     * Procedura che scrive una sezione di un documento ricevuta da un socket, su un file.
     *
     * @param socket socket da cui leggere
     * @param username username del proprietario del documento o dell'utente che ha fatto richiesta di ricevere una sezione
     * @param isServer booleano che indica se la richiesta è fatta dal server o dal client {TRUE = server, FALSE = client}
     *
     * @throws IOException errore di I/O sul socket o filepath errato (sezione non trovata)
     */
    public static void transferFromSection(SocketChannel socket, String username, boolean isServer) throws IOException
    {
        // leggo il path
        byte[] pathBytes = recvBytes(socket);
        String filepath = new String(pathBytes);

        filepath = ((isServer) ? Utils.SERVER_FILES_PATH : Utils.CLIENT_FILES_PATH) + username + "/" + filepath;

        // creo la directory (se non esiste già) che conterrà il file che sto per ricevere
        Files.createDirectories(Paths.get(filepath.substring(0,filepath.lastIndexOf("/"))));

        try{ Files.createFile(Paths.get(filepath)); }
        catch(FileAlreadyExistsException ignored) {}

        FileOutputStream fos = new FileOutputStream(filepath);
        FileChannel fc = fos.getChannel();

        // leggo la lunghezza del documento
        int fileLength = recvLength(socket);

        if(fileLength != 0)
        {
            // leggo il documento
            int n = 0;
            do{ n += fc.transferFrom(socket,0, fileLength); }
            while(n < fileLength);
        }

        fc.close();
        fos.close();
    }

    /* --------------------------------- */

    /**
     * Funzione che restituisce il percorso in cui è salvato un documento o una sua sezione.
     *
     * @param username username dell'utente che ha richiesto il documento (client), o proprietario del documento (server)
     * @param filename nome del documento
     * @param section numero di sezione richiesta
     * @param isServer indica se la richiesta è fatta dal client o dal server { TRUE = server, FALSE = client}
     *
     * @return il percorso in cui è salvato il documento
     */
    public static String getPath(String username, String filename, int section, boolean isServer)
    {
        return ((isServer) ? SERVER_FILES_PATH : CLIENT_FILES_PATH) +
                username + "/" + filename +
                ((section == 0) ? "" : ("/" + filename + section + ".txt"));
    }

    /**
     * Procedura ausiliaria che svuota una directory per poi eliminarla.
     *
     * @param path percorso della directory da eliminare
     *
     * @throws IOException errore di I/O quando si accede al percorso 'path'
     */
    public static void deleteDirectory(String path) throws IOException
    {
        Files.walk(Paths.get(path))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    /* Procedure ausiliare per spostarsi tra i vari frame della GUI */
    public static void showPreviousFrame(Component c)
    {
        //ottengo il frame corrente
        MyFrame currentFrame = (MyFrame) SwingUtilities.getWindowAncestor(c);

        //ottengo il suo precedente
        MyFrame oldFrame = currentFrame.getOldFrame();

        oldFrame.setVisible(true);
        currentFrame.dispose();
    }

    public static void showNextFrame(FrameCode frame, Component c)
    {
        //ottengo il frame corrente
        MyFrame oldFrame = (MyFrame) SwingUtilities.getWindowAncestor(c);

        //mostro il frame successivo
        new MyFrame(oldFrame, frame);
    }

    /* Utility per la gestione delle aree di testo
       (chat dei client, log del server)
     */
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

    public static void printLog(String msg, OpCode requestCode, OpCode answerCode)
    {
        Utils.appendToPane(ServerPanel.logPane, msg, Color.RED,true);
        Utils.appendToPane(ServerPanel.logPane, "  REQUEST: ", Color.BLACK,false);
        Utils.appendToPane(ServerPanel.logPane, String.valueOf(requestCode), Color.BLUE,true);
        Utils.appendToPane(ServerPanel.logPane, "  RESULT: ", Color.BLACK,false);

        Color c;
        if (answerCode == OpCode.OP_OK)
            c = Color.GREEN;
        else if(answerCode == OpCode.OP_FAIL)
            c = Color.RED;
        else
            c = Color.PINK;

        Utils.appendToPane(ServerPanel.logPane, answerCode + "\n", c,true);
    }

    /**
     * Procedura che effettua l'invio di un messaggio sul socket multicast dell chat degli utenti
     * che editano uno stesso documento.
     *
     * @param sender username del mittente del messaggio
     * @param msg corpo del messaggio
     * @param address indirizzo multicast su cui mandare il messaggio (può essere NULL)
     * @param component riferimento al componente grafico su cui visualizzare un popup in caso di errore (può essere NULL)
     */
    public static void sendChatMessage(String sender, String msg, String address, Component component)
    {
        // se address è
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
