import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;

public class ManagePanel extends JPanel implements ActionListener, ItemListener {

    private JComboBox name;
    private JSpinner nsection;
    private frameCode operation;
    private ArrayList<String> clientFiles;

    private String[] options;
    private String[] titles;
    private String[] owners;
    private int[] max_sections;

    private String usr;
    private String psw;

    public ManagePanel(frameCode operation)
    {
        this.clientFiles = TuringPanel.clientFiles;
        this.operation = operation;
        this.usr = LoginPanel.usr;
        this.psw = LoginPanel.psw;


        this.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel();
        JPanel centerPanel = new JPanel();
        JPanel southPanel = new JPanel();


        String text = "Inserisci i dati del documento da ";
        if(operation == frameCode.SHOW)
            text += "visualizzare";
        else
            text += "modificare";

        northPanel.add(new JLabel(text));

        centerPanel.setLayout(new GridLayout(2,2));

        int size = clientFiles.size();
        options = new String[size];
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

        if(operation == frameCode.SHOW)
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
        int section;

        if(e.getSource() instanceof JButton)
        {
            frameCode nextFrame = frameCode.TURING;
            switch (cmd)
            {
                case "OK":
                case "SHOW ALL":
                {
                    section = (Integer) nsection.getValue();
                    boolean goback = true;
                    filename = titles[name.getSelectedIndex()];
                    owner = owners[name.getSelectedIndex()];

                    opCode op = (!cmd.equals("SHOW ALL")) ? opCode.valueOf(operation.toString()) : opCode.SHOW_ALL;

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
                            JOptionPane.showMessageDialog(this,"Sezione in modifica da parte di un altro utente","WARNING",JOptionPane.WARNING_MESSAGE);
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
                            if(operation == frameCode.EDIT)
                            {
                                nextFrame = frameCode.TURING_EDIT;
                                TuringPanel.editingFilename = filename + "_" + owner + "_" + section;
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

    public void itemStateChanged(ItemEvent e)
    {
        if(e.getStateChange() == ItemEvent.SELECTED)
        {
            int max = max_sections[name.getSelectedIndex()];
            SpinnerNumberModel model = new SpinnerNumberModel(1, 1, max, 1);
            nsection.setModel(model);
        }
    }
}