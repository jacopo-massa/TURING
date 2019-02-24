import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class MyFrame extends JFrame
{
    MyFrame(Point location, frameCode frame)
    {
        super("TURING");
        Container c = this.getContentPane();
        JPanel p;

        switch (frame)
        {
            case TURING:
            case TURING_EDIT:
                p = new TuringPanel(frame);
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

        if(location == null)
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
}
