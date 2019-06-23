package GUI;

import Client.MainClient;
import Utils.OpCode;
import Utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class ManagePanel extends JPanel implements ActionListener, ItemListener, KeyListener {

    private JComboBox name;
    private JSpinner nsection;
    private FrameCode operation;

    private String[] titles;
    private String[] owners;
    private int[] max_sections;

    private String usr;

    ManagePanel(FrameCode operation)
    {
        ArrayList<String> clientFiles = TuringPanel.clientFiles;
        this.operation = operation;
        this.usr = MainClient.username;


        this.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel();
        JPanel centerPanel = new JPanel();
        JPanel southPanel = new JPanel();


        String text = "Inserisci i dati del documento da ";
        if(operation == FrameCode.SHOW)
            text += "visualizzare";
        else
            text += "modificare";

        northPanel.add(new JLabel(text));

        centerPanel.setLayout(new GridLayout(2,2));

        int size = clientFiles.size();
        String[] options = new String[size];
        titles = new String[size];
        owners = new String[size];
        max_sections = new int[size];

        for (int i = 0; i < size; i++)
        {
            String[] o = clientFiles.get(i).split("_",3);

            titles[i] = o[0];
            owners[i] = o[1];
            max_sections[i] = Integer.parseInt(o[2]);
            options[i] = titles[i] + " - " + ((owners[i].equals(usr)) ? "(me)" : ("(" + owners[i] + ")"));
        }

        name = new JComboBox(options);
        name.addItemListener(this);

        centerPanel.add(new JLabel("Documento: "));
        centerPanel.add(name);

        centerPanel.add(new JLabel("N° sezioni: "));
        SpinnerNumberModel model = new SpinnerNumberModel(1,1,max_sections[0],1);
        nsection = new JSpinner(model);
        centerPanel.add(nsection);

        if(operation == FrameCode.SHOW)
        {
            JButton showButton = new JButton("Show All");
            southPanel.add(showButton);
            showButton.addActionListener(this);
        }

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Annulla");

        southPanel.add(okButton);
        southPanel.add(cancelButton);
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

        this.add(northPanel,BorderLayout.NORTH);
        this.add(centerPanel,BorderLayout.CENTER);
        this.add(southPanel,BorderLayout.SOUTH);
    }

    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand().toUpperCase();
        String filename, owner;

        if(e.getSource() instanceof JButton)
        {
            switch (cmd)
            {
                case "OK":
                case "SHOW ALL":
                {
                    boolean goback = true;
                    boolean canreceive = false;

                    int selectedIndex = name.getSelectedIndex();
                    int section;
                    filename = titles[selectedIndex];
                    owner = owners[selectedIndex];

                    section = (!cmd.equals("SHOW ALL")) ? (Integer) nsection.getValue() : max_sections[selectedIndex];
                    OpCode op = (!cmd.equals("SHOW ALL")) ? OpCode.valueOf(operation.toString()) : OpCode.SHOW_ALL;

                    switch(MainClient.manageDocument(op, filename, owner, section))
                    {
                        case OP_FAIL:
                        {
                            JOptionPane.showMessageDialog(this,"Errore generico nella gestione del file","ERROR",JOptionPane.ERROR_MESSAGE);
                            goback = false;
                            break;
                        }

                        case ERR_FILE_NOT_EXISTS:
                        {
                            JOptionPane.showMessageDialog(this,"File/sezione non esistente","WARNING",JOptionPane.WARNING_MESSAGE);
                            goback = false;
                            break;
                        }

                        case SECTION_EDITING:
                        {
                            if(!operation.equals(FrameCode.EDIT))
                                canreceive = true;

                            JOptionPane.showMessageDialog(this,"Sezione in fase di editing","WARNING",JOptionPane.WARNING_MESSAGE);
                            goback = false;

                            break;
                        }

                        case ERR_PERMISSION_DENIED:
                        {
                            JOptionPane.showMessageDialog(this,"Non si dispone dei permessi necessari per gestire questo file","WARNING",JOptionPane.WARNING_MESSAGE);
                            goback = false;
                            break;
                        }

                        case OP_OK:
                        {
                            canreceive = true;

                            if(operation == FrameCode.EDIT)
                            {
                                TuringPanel.editingFilename = filename + "_" + owner + "_" + section;
                                TuringPanel.editButton.setEnabled(false);
                                TuringPanel.endEditButton.setEnabled(true);
                                Utils.sendChatMessage(usr,"joined the chat", TuringPanel.editingFileAddress,this);

                            }
                        }
                    }

                    if(canreceive)
                    {
                        boolean showAll = (cmd.equals("SHOW ALL"));
                        ArrayList<Integer> editedSections = new ArrayList<>();

                        for (int i = (showAll) ? 1 : section; i <= section; i++)
                        {
                            if (MainClient.recvSection(filename, owner, i) == OpCode.SECTION_EDITING)
                                editedSections.add(i);
                        }

                        if(showAll)
                        {
                            String msg;
                            if(editedSections.size() == 0)
                                msg = "NESSUNA SEZIONE in fase di editing\n";
                            else
                            {
                                msg = "Le seguenti sezioni sono in fase di editing: \n";
                            }
                            for (int i: editedSections)
                                msg += " - Sezione " + i + "\n";

                            JOptionPane.showMessageDialog(this,msg,"INFORMATION",JOptionPane.INFORMATION_MESSAGE);
                        }
                    }

                    if(!goback)
                        break;
                }

                case "ANNULLA":
                {
                    Utils.showPreviousFrame(this);
                    break;
                }
            }
        }
    }

    public void itemStateChanged(ItemEvent e)
    {
        if(e.getStateChange() == ItemEvent.SELECTED)
        {
            int max = max_sections[name.getSelectedIndex()];
            SpinnerNumberModel model = new SpinnerNumberModel(1, 1, max, 1);
            nsection.setModel(model);
        }
    }

    public void keyTyped(KeyEvent e) {}

    public void keyPressed(KeyEvent e)
    {
        if(e.getKeyCode() == KeyEvent.VK_ESCAPE)
            Utils.showPreviousFrame(this);
    }

    public void keyReleased(KeyEvent e) {}
}