package Client;

import Utils.Message;
import Utils.Utils;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

public class InviteTask implements Runnable
{
    // socket su cui ascoltare la ricezione degli inviti
    private SocketChannel socketChannel;

    /**
     * Invite è il target eseguito dal thread listener degli inviti, istanziato in MainClient.
     *
     * @param socketChannel socket su cui ascoltare la ricezione degli inviti
     */
    InviteTask(SocketChannel socketChannel)
    {
        this.socketChannel = socketChannel;
    }

    /**
     * metodo eseguito dal thread degli inviti quando viene fatto partire.
     */
    public void run()
    {
        while(!Thread.interrupted())
        {
            // scarico l'invito
            try
            {
                Message invitation = (Message) Utils.recvObject(socketChannel);
                if(invitation == null)
                    throw new NullPointerException();

                // stampo l'invito
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
