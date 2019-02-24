import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.awt.*;
import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

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
                Invitation invitation = (Invitation) Utils.recvObject(socketChannel);
                if(invitation == null)
                    throw new NullPointerException();

                Date date = new Date();
                Utils.printInvite(invitation.getSender(), invitation.getFilename(), invitation.getDate());

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
