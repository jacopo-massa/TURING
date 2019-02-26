package GUI;

import Utils.Utils;
import Client.MainClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CreationPanel extends JPanel implements ActionListener, KeyListener
{

    private JTextField name;
    private JSpinner nsection;

    CreationPanel()
    {
        this.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel();
        JPanel centerPanel = new JPanel();
        JPanel southPanel = new JPanel();

        northPanel.add(new JLabel("Inserisci i dati del documento da "));

        centerPanel.setLayout(new GridLayout(2,2));

        centerPanel.add(new JLabel("Nome documento: "));
        name = new JTextField();
        centerPanel.add(name);

        centerPanel.add(new JLabel("N° sezioni: "));
        SpinnerNumberModel model = new SpinnerNumberModel(1,1,100,1);
        nsection = new JSpinner(model);
        centerPanel.add(nsection);

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Annulla");

        southPanel.add(okButton);
        southPanel.add(cancelButton);
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

        name.addKeyListener(this);

        this.add(northPanel,BorderLayout.NORTH);
        this.add(centerPanel,BorderLayout.CENTER);
        this.add(southPanel,BorderLayout.SOUTH);
    }

    private void create()
    {
        String filename = name.getText();
        int section = (Integer) nsection.getValue();

        if (filename.equals(""))
        {
            JOptionPane.showMessageDialog(this, "Nome del file necessario!", "WARNING", JOptionPane.WARNING_MESSAGE);
        }
        else
        {
            boolean goback = true;
            switch (MainClient.createDocument(filename, section))
            {
                case ERR_FILE_ALREADY_EXISTS:
                {
                    JOptionPane.showMessageDialog(this, "File già esistente!", "WARNING", JOptionPane.WARNING_MESSAGE);
                    goback = false;
                    break;
                }

                case OP_FAIL:
                {
                    JOptionPane.showMessageDialog(this, "Errore generico nella gestione del file", "ERROR", JOptionPane.ERROR_MESSAGE);
                    break;
                }

                case OP_OK:
                {
                    JOptionPane.showMessageDialog(this, "File creato con successo", "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
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
            create();
        else if("ANNULLA".equals(cmd))
            Utils.showPreviousFrame(this);

    }

    public void keyTyped(KeyEvent e) {}

    public void keyPressed(KeyEvent e)
    {
        if(e.getKeyCode() == KeyEvent.VK_ENTER)
            create();
        else if(e.getKeyCode() == KeyEvent.VK_ESCAPE)
            Utils.showPreviousFrame(this);
    }

    public void keyReleased(KeyEvent e) {}
}