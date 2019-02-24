import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class InvitationPanel extends JPanel implements ActionListener
{

    private JComboBox filename;
    private JTextField collaborator;

    private ArrayList<String> clientFiles;

    private String[] titles;
    private String[] owners;

    private String usr;
    private String psw;

    public InvitationPanel()
    {
        this.clientFiles = TuringPanel.clientFiles;

        int size = clientFiles.size();
        titles = new String[size];
        owners = new String[size];

        for (int i = 0; i < size; i++)
        {
            String[] o = clientFiles.get(i).split("_",3);
            titles[i] = o[0];
            owners[i] = o[1];

            //TODO - invitare solo per file miei
            /*if(o[1].equals(usr))
            {
                titles[i] = o[0];
                owners[i] = o[1];
            }*/
        }

        /*System.out.println(titles.length);
        if(titles.length == 0)
            JOptionPane.showMessageDialog(this,"Nessun file a cui poter collaborare!","WARNING",JOptionPane.WARNING_MESSAGE);
        else
        {*/
            this.usr = LoginPanel.usr;
            this.psw = LoginPanel.psw;


            this.setLayout(new BorderLayout());
            JPanel northPanel = new JPanel();
            JPanel centerPanel = new JPanel();
            JPanel southPanel = new JPanel();

            northPanel.add(new JLabel("Scegli il documento e l'utente da invitare"));

            centerPanel.setLayout(new GridLayout(2,2));

            filename = new JComboBox(titles);

            centerPanel.add(new JLabel("Documento: "));
            centerPanel.add(filename);

            centerPanel.add(new JLabel("Username collaboratore: "));
            collaborator = new JTextField();
            centerPanel.add(collaborator);

            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Annulla");

            southPanel.add(okButton);
            southPanel.add(cancelButton);
            okButton.addActionListener(this);
            cancelButton.addActionListener(this);

            this.add(northPanel,BorderLayout.NORTH);
            this.add(centerPanel,BorderLayout.CENTER);
            this.add(southPanel,BorderLayout.SOUTH);
        //}
    }
    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand().toUpperCase();
        frameCode nextFrame = frameCode.TURING;

        switch (cmd)
        {
            case "OK":
            {
                String chosenCollaborator = collaborator.getText();
                boolean goback = true;

                if(chosenCollaborator.isEmpty())
                {
                    JOptionPane.showMessageDialog(this,"Username collaboratore necessario!","WARNING",JOptionPane.WARNING_MESSAGE);
                    goback = false;
                }
                else if(chosenCollaborator.equals(usr))
                {
                    JOptionPane.showMessageDialog(this,"Non puoi invitare te stesso!","WARNING",JOptionPane.WARNING_MESSAGE);
                    goback = false;
                }
                else
                {
                    String chosenFilename = titles[filename.getSelectedIndex()];
                    switch (MainClient.invite(chosenFilename,chosenCollaborator))
                    {
                        case OP_FAIL:
                        {
                            JOptionPane.showMessageDialog(this,"Errore nell'invio dell'invito","ERROR",JOptionPane.ERROR_MESSAGE);
                            break;
                        }

                        case ERR_USER_UNKNOWN:
                        {
                            JOptionPane.showMessageDialog(this,"Utente sconosciuto!","WARNING",JOptionPane.WARNING_MESSAGE);
                            goback = false;
                            break;
                        }

                        case ERR_USER_ALREADY_INVITED:
                        {
                            JOptionPane.showMessageDialog(this,"Utente già invitato!","WARNING",JOptionPane.WARNING_MESSAGE);
                            goback = false;
                            break;
                        }

                        case ERR_OWNER_INVITED:
                        {
                            {
                                JOptionPane.showMessageDialog(this,"Non puoi invitare il proprietario del file!","WARNING",JOptionPane.WARNING_MESSAGE);
                                goback = false;
                                break;
                            }
                        }

                        case OP_OK:
                        {
                            JOptionPane.showMessageDialog(this,"Invito mandato con successo","SUCCESS",JOptionPane.INFORMATION_MESSAGE);
                            break;
                        }
                    }
                }

                if(!goback)
                    break;
            }
            case "ANNULLA":
            {
                Utils.showNextFrame(nextFrame,this);
                break;
            }
        }

    }
}
