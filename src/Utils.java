import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

class Utils
{

    static String CLIENT_FILES_PATH = "./client_files/";
    static String SERVER_FILES_PATH = "./turing_files/";

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
        socket.write(wrap);
    }

    static Serializable recvObject(SocketChannel socket) throws IOException, ClassNotFoundException
    {
        ByteBuffer lengthByteBuffer = ByteBuffer.wrap(new byte[4]);
        ByteBuffer dataByteBuffer = null;

        socket.read(lengthByteBuffer);
        if (lengthByteBuffer.remaining() == 0)
        {
            dataByteBuffer = ByteBuffer.allocate(lengthByteBuffer.getInt(0));
            lengthByteBuffer.clear();
        }
        socket.read(dataByteBuffer);
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

    static void showNextFrame(frameCode frame, Component c)
    {
        //nascondo il frame corrente
        MyFrame old_f = (MyFrame) SwingUtilities.getWindowAncestor(c);
        old_f.setVisible(false);
        //mostro il frame successivo
        new MyFrame(frame);
    }

    private static void sendLength(SocketChannel socket, int length) throws IOException
    {
        ByteBuffer dimBuffer = ByteBuffer.wrap(new byte[4]);

        dimBuffer.clear();
        dimBuffer.putInt(length);
        dimBuffer.flip();

        socket.write(dimBuffer);
    }

    private static int recvLength(SocketChannel socket) throws IOException
    {
        ByteBuffer dimBuffer = ByteBuffer.wrap(new byte[4]);

        socket.read(dimBuffer);
        System.out.println(dimBuffer.toString());
        dimBuffer.flip();
        int dim = dimBuffer.getInt(0);
        dimBuffer.clear();

        return dim;
    }

    static void sendBytes(SocketChannel socket, byte[] bytes) throws IOException
    {
        ByteBuffer bytesBuffer;

        sendLength(socket,bytes.length);

        bytesBuffer = ByteBuffer.wrap(bytes);

        socket.write(bytesBuffer);

        bytesBuffer.clear();
    }

    static byte[] recvBytes(SocketChannel socket) throws IOException
    {
        //leggo la dimensione del code che sto per ricevere
        ByteBuffer bytesBuffer;

        int dim = recvLength(socket);

        bytesBuffer = ByteBuffer.allocate(dim);
        socket.read(bytesBuffer);
        bytesBuffer.flip();

        byte[] answerBytes = new byte[bytesBuffer.remaining()];
        bytesBuffer.get(answerBytes);

        return answerBytes;

    }

    static void transferToSection(SocketChannel socket, String filepath) throws IOException
    {

        File f = new File(filepath);
        FileInputStream fis;
        FileChannel fc;

        try
        {
            fis = new FileInputStream(f);
            fc = fis.getChannel();
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
        sendLength(socket, (int) f.length());


        //mando il file
        fc.transferTo(0, f.length(), socket);
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

        //leggo il file
        fc.transferFrom(socket,0, fileLength);
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
}
