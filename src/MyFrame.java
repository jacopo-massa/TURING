import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class MyFrame extends JFrame
{
    MyFrame(frameCode frame)
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

            case LOGIN:
            default:
                p = new LoginPanel();
                break;

        }

        c.add(p);

        //minimizzo la dimensione della finestra
        this.pack();

        //imposto posizione della finestra
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation((screenSize.width - this.getWidth())/2,(screenSize.height - this.getHeight())/2);

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
