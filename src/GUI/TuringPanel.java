package GUI;

import Client.MainClient;
import Utils.Message;
import Utils.Operation;
import Utils.Utils;
import Utils.opCode;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;

public class TuringPanel extends JPanel implements ActionListener, KeyListener
{
    private String usr;
    private String psw;

    static JButton editButton;
    static JButton endEditButton;
    private static JTextField sendArea;
    public static JTextPane receiveArea;

    static ArrayList<String> clientFiles;
    static String editingFilename;
    public static String editingFileAddress;

    TuringPanel()
    {
        this.usr = MainClient.username;
        this.psw = MainClient.password;
        ArrayList<Message> pendingInvitations = MainClient.pendingInvitations;


        this.setLayout(new BorderLayout());

        /* --- WEST PANEL --- */
        JPanel westPanel = new JPanel();
        westPanel.setLayout(new BorderLayout());

        receiveArea = new JTextPane();
        receiveArea.setEditable(false);
        receiveArea.setPreferredSize(new Dimension(400,270));
        receiveArea.setMargin(new Insets(5,5,5,5));

        JScrollPane scrollPane = new JScrollPane(receiveArea);

        if(pendingInvitations != null)
        {
            for (Message i: pendingInvitations)
                Utils.printInvite(i.getSender(), i.getBody(), i.getDate());
        }


        JPanel westSouthPanel = new JPanel();
        westSouthPanel.setLayout(new GridLayout(1,2));
        sendArea = new JTextField(15);
        sendArea.setCaret(new DefaultCaret());
        sendArea.addKeyListener(this);
        JButton sendButton = new JButton("Invia");

        westPanel.add(scrollPane,BorderLayout.WEST);
        westSouthPanel.add(sendArea);
        westSouthPanel.add(sendButton);
        westPanel.add(westSouthPanel,BorderLayout.SOUTH);

        /* --- EAST PANEL --- */
        JPanel eastPanel = new JPanel();
        eastPanel.setLayout(new GridLayout(6,1));

        JButton createButton = new JButton("Create");
        editButton = new JButton("Edit");
        editButton.setEnabled(true);
        endEditButton = new JButton("End Edit");
        endEditButton.setEnabled(false);
        JButton showButton = new JButton("Show");
        JButton inviteButton = new JButton("Invite");
        JButton logoutButton = new JButton("Logout");

        createButton.addActionListener(this);
        editButton.addActionListener(this);
        endEditButton.addActionListener(this);
        showButton.addActionListener(this);
        inviteButton.addActionListener(this);
        logoutButton.addActionListener(this);
        sendButton.addActionListener(this);

        eastPanel.add(createButton);
        eastPanel.add(editButton);
        eastPanel.add(endEditButton);
        eastPanel.add(showButton);
        eastPanel.add(inviteButton);
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
                case "INVIA":
                {
                    Utils.sendChatMessage(usr,sendArea.getText(),editingFileAddress,this);
                    sendArea.setText("");
                    break;
                }
                case "LOGOUT":
                {
                    switch(MainClient.logoutUser())
                    {
                        case OP_OK:
                        {
                            //Utils.showNextFrame(frameCode.LOGIN,this);
                            Utils.showPreviousFrame(this);
                            break;
                        }

                        case OP_FAIL:
                            JOptionPane.showMessageDialog(this,"Errore nella comunicazione col server","ERROR",JOptionPane.ERROR_MESSAGE);
                            break;
                    }
                    break;
                }

                case "EDIT":
                case "SHOW":
                case "INVITE":
                {
                    // richiedo al server la lista di documenti gestibili

                    Operation request = new Operation(usr);
                    request.setPassword(psw);
                    request.setCode(opCode.FILE_LIST);
                    opCode ans;
                    /* se voglio effettuare un invito, mi assicuro di ricevere solo i documenti
                       di cui sono proprietario, impostanto owner = username, come si aspetta il server
                      */
                    if(cmd.equals("INVITE"))
                        request.setOwner(usr);

                    MainClient.sendReq(MainClient.clientSocketChannel,request);

                    try
                    {
                        clientFiles = (ArrayList<String>) Utils.recvObject(MainClient.clientSocketChannel);

                        if(clientFiles == null)
                            throw new IOException();
                    }
                    catch(ClassNotFoundException | IOException ex)
                    {
                        System.err.println("Can't download file list");
                        ex.printStackTrace();
                        ans = opCode.OP_FAIL;
                    }

                    ans = MainClient.getAnswer();

                    if(ans != opCode.OP_OK)
                    {
                        JOptionPane.showMessageDialog(this,"Lista dei file gestibili non scaricata correttamente","WARNING",JOptionPane.WARNING_MESSAGE);
                        break;
                    }

                    if(clientFiles.size() == 0)
                    {
                        JOptionPane.showMessageDialog(this,"Nessun file da gestire!","WARNING",JOptionPane.WARNING_MESSAGE);
                        break;
                    }
                }
                case "CREATE":
                {
                    Utils.showNextFrame(frameCode.valueOf(cmd),this);
                    break;
                }

                case "END EDIT":
                {
                    String[] parts = editingFilename.split("_",3);
                    switch (MainClient.endEditDocument(parts[0],parts[1],Integer.parseInt(parts[2])))
                    {
                        case OP_OK:
                        {
                            JOptionPane.showMessageDialog(this,"Documento aggiornato con successo","SUCCESS",JOptionPane.INFORMATION_MESSAGE);
                            editButton.setEnabled(true);
                            endEditButton.setEnabled(false);
                            editingFilename = null;

                            Utils.sendChatMessage(usr,"left the chat", editingFileAddress,this);
                            editingFileAddress = null;
                            break;
                        }
                        case OP_FAIL:
                            JOptionPane.showMessageDialog(this,"Errore nell'aggiornamento del documento","ERROR",JOptionPane.ERROR_MESSAGE);
                            break;
                    }
                    break;
                }
            }
        }
    }

    public void keyTyped(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}

    public void keyPressed(KeyEvent e)
    {
        if(e.getKeyCode() == KeyEvent.VK_ENTER)
        {
            Utils.sendChatMessage(usr,sendArea.getText(),editingFileAddress,this);
            sendArea.setText("");
        }
    }
}