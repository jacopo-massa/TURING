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
}
