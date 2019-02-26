package GUI;

import Client.MainClient;
import Utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

public class InvitationPanel extends JPanel implements ActionListener, KeyListener
{

    private JComboBox filename;
    private JTextField collaborator;

    private String[] titles;

    private String usr;

    InvitationPanel()
    {
        this.usr = MainClient.username;
        ArrayList<String> clientFiles = TuringPanel.clientFiles;

        int size = clientFiles.size();
        titles = new String[size];

        for (int i = 0; i < size; i++)
        {
            String[] o = clientFiles.get(i).split("_",3);

            titles[i] = o[0];
        }

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

        collaborator.addKeyListener(this);

        this.add(northPanel,BorderLayout.NORTH);
        this.add(centerPanel,BorderLayout.CENTER);
        this.add(southPanel,BorderLayout.SOUTH);
    }

    private void invite()
    {
        String chosenCollaborator = collaborator.getText();

        if (chosenCollaborator.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Username collaboratore necessario!", "WARNING", JOptionPane.WARNING_MESSAGE);
        }
        else if (chosenCollaborator.equals(usr))
        {
            JOptionPane.showMessageDialog(this, "Non puoi invitare te stesso!", "WARNING", JOptionPane.WARNING_MESSAGE);
        }
        else
        {
            boolean goback = true;
            String chosenFilename = titles[filename.getSelectedIndex()];

            switch (MainClient.invite(chosenFilename, chosenCollaborator))
            {
                case OP_FAIL:
                {
                    JOptionPane.showMessageDialog(this, "Errore nell'invio dell'invito", "ERROR", JOptionPane.ERROR_MESSAGE);
                    break;
                }

                case ERR_USER_UNKNOWN:
                {
                    JOptionPane.showMessageDialog(this, "Utente sconosciuto!", "WARNING", JOptionPane.WARNING_MESSAGE);
                    goback = false;
                    break;
                }

                case ERR_USER_ALREADY_INVITED:
                {
                    JOptionPane.showMessageDialog(this, "Utente già invitato!", "WARNING", JOptionPane.WARNING_MESSAGE);
                    goback = false;
                    break;
                }

                case ERR_OWNER_INVITED:
                {
                    JOptionPane.showMessageDialog(this, "Non puoi invitare il proprietario del file!", "WARNING", JOptionPane.WARNING_MESSAGE);
                    goback = false;
                    break;
                }

                case OP_OK:
                {
                    JOptionPane.showMessageDialog(this, "Invito mandato con successo", "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
                    break;
                }
            }

            if(goback)
                Utils.showPreviousFrame(this);
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand().toUpperCase();
        if ("OK".equals(cmd))
            invite();
        else if("ANNULLA".equals(cmd))
            Utils.showPreviousFrame(this);
    }

    public void keyTyped(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}

    public void keyPressed(KeyEvent e)
    {
        if(e.getKeyCode() == KeyEvent.VK_ENTER)
            invite();
        else if(e.getKeyCode() == KeyEvent.VK_ESCAPE)
            Utils.showPreviousFrame(this);
    }
}
