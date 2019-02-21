import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Utils
{
    public static void sendObject(SocketChannel socket, Serializable serializable) throws IOException
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

    public static Serializable recvObject(SocketChannel socket) throws IOException, ClassNotFoundException
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

    public static void showNextFrame(String frame, Component c)
    {
        //nascondo il frame corrente
        MyFrame old_f = (MyFrame) SwingUtilities.getWindowAncestor(c);
        old_f.setVisible(false);
        //mostro il frame successivo
        MyFrame f = new MyFrame(frame);
    }

    public static void sendOpCode(SocketChannel socket, opCode code) throws IOException
    {
        ByteBuffer dim = ByteBuffer.allocate(4);
        ByteBuffer codeBuffer;

        //invio prima la dimensione del code.

        dim.clear();
        dim.putInt(code.toString().length());
        dim.flip();

        socket.write(dim);

        //invio il code come stringa
        byte[] codeBytes = code.toString().getBytes();
        codeBuffer = ByteBuffer.wrap(codeBytes);

        socket.write(codeBuffer);
    }

    public static opCode recvOpCode(SocketChannel socket) throws IOException
    {
        //leggo la dimensione del code che sto per ricevere
        ByteBuffer dimBuffer = ByteBuffer.wrap(new byte[4]);
        ByteBuffer codeBuffer;

        socket.read(dimBuffer);
        dimBuffer.flip();
        int dim = dimBuffer.getInt();
        dimBuffer.clear();

        codeBuffer = ByteBuffer.allocate(dim);
        socket.read(codeBuffer);
        codeBuffer.flip();

        byte[] codeBytes = new byte[codeBuffer.remaining()];
        codeBuffer.get(codeBytes);

        return opCode.valueOf(new String(codeBytes));

    }
}
