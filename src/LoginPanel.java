import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LoginPanel extends JPanel implements ActionListener
{
    private JTextField username;
    private JPasswordField password;

    protected static String usr;
    protected static String psw;

    public LoginPanel()
    {
        this.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel();
        JPanel centerPanel = new JPanel();
        JPanel southPanel = new JPanel();

        northPanel.add(new JLabel("Benvenuto in TURING!"));

        centerPanel.setLayout(new GridLayout(2,2));

        centerPanel.add(new JLabel("Username: "));
        username = new JTextField();
        centerPanel.add(username);

        centerPanel.add(new JLabel("Password: "));
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
            usr = this.username.getText();
            psw = new String(this.password.getPassword());
            if(usr.equals("") || psw.equals(""))
            {
                JOptionPane.showMessageDialog(this,"Inserisci username/password!","ERRORE",JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                MainClient.username = usr;
                MainClient.password = psw;
                if(cmd.equals("REGISTRAZIONE"))
                {
                    switch (MainClient.register())
                    {
                        case 1: {
                            //nascondo il frame di login
                            MyFrame old_f = (MyFrame) SwingUtilities.getWindowAncestor(this);
                            old_f.setVisible(false);
                            //mostro il nuovo frame, contenente la chat e i button per le richieste
                            MyFrame f = new MyFrame("turing");
                            break;
                        }

                        case 0:
                            JOptionPane.showMessageDialog(this, "Esiste già un utente con questo username! (" + usr + ")", "WARNING", JOptionPane.WARNING_MESSAGE);
                            break;

                        case -1:
                            JOptionPane.showMessageDialog(this, "Errore durante la registrazione!", "ERROR", JOptionPane.ERROR_MESSAGE);
                            break;
                    }
                }
                else if(cmd.equals("LOGIN"))
                {
                    switch (MainClient.loginUser())
                    {
                        case OP_OK:
                        {
                            //nascondo il frame di login
                            Utils.showNextFrame("TURING",this);
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
