import com.sun.tools.javac.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TuringPanel extends JPanel implements ActionListener
{
    private JButton logoutButton;
    private JButton sendButton;
    private JButton createButton;
    private JButton editButton;
    private JButton endEditButton;
    private JButton showButton;

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
        eastPanel.setLayout(new GridLayout(5,1));

        createButton = new JButton("Create");
        editButton = new JButton("Edit");
        endEditButton = new JButton("End Edit");
        endEditButton.setEnabled(false);
        showButton = new JButton("Show");
        logoutButton = new JButton("Logout");

        createButton.addActionListener(this);
        editButton.addActionListener(this);
        endEditButton.addActionListener(this);
        showButton.addActionListener(this);
        logoutButton.addActionListener(this);

        eastPanel.add(createButton);
        eastPanel.add(editButton);
        eastPanel.add(endEditButton);
        eastPanel.add(showButton);
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
                    switch(MainClient.logoutUser())
                    {
                        case OP_OK:
                        {
                            Utils.showNextFrame("login",this);
                            break;
                        }

                        case OP_FAIL:
                            JOptionPane.showMessageDialog(this,"Errore nella comunicazione col server","ERROR",JOptionPane.ERROR_MESSAGE);
                            break;
                    }
                    break;
                }

                case "CREATE":
                case "EDIT":
                case "SHOW":
                {
                    Utils.showNextFrame(cmd,this);
                    break;
                }
            }
        }
    }
}