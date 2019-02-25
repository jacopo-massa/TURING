import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class MyFrame extends JFrame
{
    private MyFrame oldFrame;
    private Point location;
    private JPanel p;

    MyFrame(MyFrame oldFrame, frameCode frame)
    {
        super("TURING");
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

            case LOGIN:
            default:
                p = new LoginPanel();
                break;

        }

        c.add(p);

        //minimizzo la dimensione della finestra
        this.pack();

        //imposto posizione della finestra
        if(oldFrame != null && oldFrame.isVisible())
        {
            location = oldFrame.getLocationOnScreen();
            oldFrame.setVisible(false);
        }
        else
        {
            Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
            location = new Point(
                    (int) (dimension.getWidth() - this.getWidth()) / 2,
                    (int) (dimension.getHeight() - this.getHeight()) / 2);
        }
        this.setLocation(location);

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
