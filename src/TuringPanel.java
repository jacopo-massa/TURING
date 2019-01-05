import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TuringPanel extends JPanel implements ActionListener
{
    private JButton logoutButton;
    private JButton sendButton;
    private JButton createButton;
    private JButton endEditButton;

    private JTextArea receiveArea;
    private JTextField sendArea;

    private String usr;
    private String psw;

    public TuringPanel()
    {
        this.usr = LoginPanel.usr;
        this.psw = LoginPanel.psw;
        this.setLayout(new BorderLayout());

        /* --- WEST PANEL --- */
        JPanel westPanel = new JPanel();
        westPanel.setLayout(new BorderLayout());

        receiveArea = new JTextArea(20,28);
        receiveArea.setEditable(false);

        JPanel westSouthPanel = new JPanel();
        westSouthPanel.setLayout(new GridLayout(1,2));
        sendArea = new JTextField(15);
        sendButton = new JButton("Invia");

        westPanel.add(receiveArea,BorderLayout.WEST);
        westSouthPanel.add(sendArea);
        westSouthPanel.add(sendButton);
        westPanel.add(westSouthPanel,BorderLayout.SOUTH);

        /* --- EAST PANEL --- */
        JPanel eastPanel = new JPanel();
        eastPanel.setLayout(new GridLayout(3,1));

        createButton = new JButton("Create Document");
        endEditButton = new JButton("End Edit");
        logoutButton = new JButton("Logout");

        createButton.addActionListener(this);
        endEditButton.addActionListener(this);
        logoutButton.addActionListener(this);


        eastPanel.add(createButton);
        eastPanel.add(endEditButton);
        eastPanel.add(logoutButton);


        this.add(westPanel,BorderLayout.WEST);
        this.add(eastPanel,BorderLayout.EAST);

        westSouthPanel.setMaximumSize(new Dimension(westPanel.getWidth()/2, westPanel.getHeight()/2));
    }
    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand().toUpperCase();

        if(e.getSource() instanceof JButton)
        {
            switch (cmd)
            {
                case "LOGOUT":
                {
                    if(MainClient.logoutUser() == 1)
                    {
                        //nascondo il frame turing
                        MyFrame old_f = (MyFrame) SwingUtilities.getWindowAncestor(this);
                        old_f.setVisible(false);
                        //mostro il frame di login
                        MyFrame f = new MyFrame("login");
                    }
                    else
                        JOptionPane.showMessageDialog(this,"Errore nella disconnessione","ERROR",JOptionPane.ERROR_MESSAGE);

                    break;
                }

                case "CREATE DOCUMENT":
                {
                    //nascondo il frame turing
                    MyFrame old_f = (MyFrame) SwingUtilities.getWindowAncestor(this);
                    old_f.setVisible(false);
                    //mostro il frame di login
                    MyFrame f = new MyFrame("creation");
                    break;
                }
            }
        }
    }
}