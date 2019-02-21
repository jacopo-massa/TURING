import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;

public class ManagePanel extends JPanel implements ActionListener, ItemListener {

    private JComboBox name;
    private JSpinner nsection;
    private String operation;
    private ArrayList<String> clientFiles;

    private String[] options;
    private String[] titles;
    private String[] owners;
    private int[] max_sections;

    private String usr;
    private String psw;

    public ManagePanel(String operation)
    {
        this.clientFiles = TuringPanel.clientFiles;
        this.operation = operation.toUpperCase();
        this.usr = LoginPanel.usr;
        this.psw = LoginPanel.psw;


        this.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel();
        JPanel centerPanel = new JPanel();
        JPanel southPanel = new JPanel();


        String text = "Inserisci i dati del documento da ";
        if(operation.equals("SHOW"))
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
        SpinnerNumberModel model = new SpinnerNumberModel(1,1,10,1);
        nsection = new JSpinner(model);
        centerPanel.add(nsection);

        if(operation.equals("SHOW"))
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
        int section = 0;

        if(e.getSource() instanceof JButton)
        {
            switch (cmd)
            {
                case "OK":
                    section = (Integer) nsection.getValue();

                case "SHOW ALL":
                {
                    boolean goback = true;
                    filename = titles[name.getSelectedIndex()];
                    owner = owners[name.getSelectedIndex()];

                    switch(MainClient.manageDocument(opCode.valueOf(operation), filename, owner, section))
                    {
                        case OP_FAIL:
                        {
                            JOptionPane.showMessageDialog(this,"Errore generico nella gestione del file","ERROR",JOptionPane.ERROR_MESSAGE);
                            goback = false;
                            break;
                        }

                        case OP_OK:
                    }

                    if(!goback)
                        break;
                }

                case "ANNULLA":
                {
                    Utils.showNextFrame("TURING",this);
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
            System.out.println("Selected " + max + " " + name.getSelectedItem());
            SpinnerNumberModel model = new SpinnerNumberModel(1, 1, max, 1);
            nsection.setModel(model);
        }
    }
}