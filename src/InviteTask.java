import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

public class InviteTask implements Runnable
{
    private SocketChannel socketChannel;

    public InviteTask(SocketChannel socketChannel)
    {
        this.socketChannel = socketChannel;
    }

    public void run()
    {
        while(true)
        {
            try
            {
                Message invitation = (Message) Utils.recvObject(socketChannel);
                if(invitation == null)
                    throw new NullPointerException();

                Utils.printInvite(invitation.getSender(), invitation.getBody(), invitation.getDate());
            }
            catch(ClosedChannelException e)
            {
                System.err.println("Invite notification interrupted");
                break;
            }
            catch (ClassNotFoundException | IOException | NullPointerException e)
            {
                System.err.println("Error in downloading invite\n");
                e.printStackTrace();
            }
        }
    }
}
