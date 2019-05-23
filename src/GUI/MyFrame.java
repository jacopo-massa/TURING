package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class MyFrame extends JFrame
{
    private MyFrame oldFrame;
    private JPanel p;

    public MyFrame(MyFrame oldFrame, frameCode frame)
    {
        super("TURING " + ( (frame == frameCode.SERVER) ? "SERVER" : "CLIENT"));
        this.oldFrame = oldFrame;
        Container c = this.getContentPane();

        switch (frame)
        {
            case TURING:
                p = new TuringPanel();
                break;

            case CREATE:
                p = new CreationPanel();
                break;

            case EDIT:
            case SHOW:
                p = new ManagePanel(frame);
                break;

            case INVITE:
                p = new InvitationPanel();
                break;

            case SERVER:
                p = new ServerPanel();
                break;

            case LOGIN:
            default:
                p = new LoginPanel();
                break;

        }

        c.add(p);

        //minimizzo la dimensione della finestra
        this.pack();

        //imposto posizione della finestra
        if(oldFrame != null)
            oldFrame.setVisible(false);

        this.setLocationRelativeTo(null);

        //all'uscita dalla finestra, chiude il processo con System Exit.
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        //rendo visibile la finestra, e non ridimensionabile
        this.setResizable(false);
        this.setVisible(true);

        this.addFocusListener(new FocusAdapter()
        {
            public void focusGained(FocusEvent aE)
            { p.requestFocusInWindow(); }
        });
    }

    public MyFrame getOldFrame() {
        return oldFrame;
    }
}