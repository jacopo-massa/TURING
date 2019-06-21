package GUI;

import Client.MainClient;
import Utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginPanel extends JPanel implements ActionListener
{
    private JTextField username;
    private JPasswordField password;

    LoginPanel()
    {
        this.setLayout(new BorderLayout(10,5));
        JPanel northPanel = new JPanel();
        JPanel centerPanel = new JPanel();
        JPanel southPanel = new JPanel();

        //northPanel.add(new JLabel("Benvenuto in TURING!"));
        northPanel.add(new JLabel(new ImageIcon("turing.png")));

        centerPanel.setLayout(new GridLayout(2,2));

        centerPanel.add(new JLabel("Username: ",JLabel.CENTER));
        username = new JTextField();
        centerPanel.add(username);

        centerPanel.add(new JLabel("Password: ",JLabel.CENTER));
        password = new JPasswordField();
        centerPanel.add(password);

        JButton login = new JButton("Login");
        JButton registrati = new JButton("Registrazione");

        southPanel.add(login);
        southPanel.add(registrati);
        login.addActionListener(this);
        registrati.addActionListener(this);

        this.add(northPanel,BorderLayout.NORTH);
        this.add(centerPanel,BorderLayout.CENTER);
        this.add(southPanel,BorderLayout.SOUTH);
    }

    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand().toUpperCase();

        if(e.getSource() instanceof JButton)
        {
            String usr = this.username.getText();
            String psw = new String(this.password.getPassword());
            if(usr.equals("") || psw.equals(""))
            {
                JOptionPane.showMessageDialog(this,"Inserisci username/password!","ERRORE",JOptionPane.ERROR_MESSAGE);
            }
            else if (usr.contains(" ") || psw.contains(" "))
            {
                JOptionPane.showMessageDialog(this,"Username e password non possono contenere spazi!","WARNING",JOptionPane.WARNING_MESSAGE);
            }
            else
            {
                MainClient.username = usr;
                MainClient.password = psw;
                if(cmd.equals("REGISTRAZIONE"))
                {
                    switch (MainClient.register())
                    {
                        case OP_OK:
                        {
                            Utils.showNextFrame(FrameCode.TURING,this);
                            break;
                        }

                        case ERR_USER_ALREADY_LOGGED:
                            JOptionPane.showMessageDialog(this, "Esiste già un utente con questo username! (" + usr + ")", "WARNING", JOptionPane.WARNING_MESSAGE);
                            break;

                        case OP_FAIL:
                            JOptionPane.showMessageDialog(this, "Errore durante la registrazione \nNon riesco a contattare il server!", "ERROR", JOptionPane.ERROR_MESSAGE);
                            break;
                    }
                }
                else if(cmd.equals("LOGIN"))
                {
                    switch (MainClient.loginUser())
                    {
                        case OP_OK:
                        {
                            Utils.showNextFrame(FrameCode.TURING,this);
                            break;
                        }

                        case ERR_USER_UNKNOWN:
                            JOptionPane.showMessageDialog(this,"Utente inesistente","WARNING",JOptionPane.WARNING_MESSAGE);
                            break;

                        case ERR_WRONG_PASSWORD:
                            JOptionPane.showMessageDialog(this,"Password errata","WARNING",JOptionPane.WARNING_MESSAGE);
                            break;

                        case ERR_USER_ALREADY_LOGGED:
                            JOptionPane.showMessageDialog(this,"Utente già connesso","WARNING",JOptionPane.WARNING_MESSAGE);
                            break;

                        case OP_FAIL:
                            JOptionPane.showMessageDialog(this,"Errore nella comunicazione col server","ERROR",JOptionPane.ERROR_MESSAGE);
                            break;
                    }
                }
            }
        }

    }
}
