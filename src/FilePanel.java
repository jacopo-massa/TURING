import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class FilePanel extends JPanel implements ActionListener
{

    private JTextField name;
    private JSpinner nsection;
    private String operation;

    public FilePanel(String operation)
    {
        this.operation = operation;
        this.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel();
        JPanel centerPanel = new JPanel();
        JPanel southPanel = new JPanel();

        String text = "Inserisci i dati del documento da ";

        switch(this.operation.toUpperCase())
        {
            case "CREATE":
                text += "creare";
                break;

            case "EDIT":
                text += "modificare";
                break;

            case "SHOW":
                text += "visualizzare";
                break;
        }

        northPanel.add(new JLabel(text));

        centerPanel.setLayout(new GridLayout(2,2));

        centerPanel.add(new JLabel("Nome documento: "));
        name = new JTextField();
        centerPanel.add(name);

        centerPanel.add(new JLabel("N° sezioni: "));
        SpinnerNumberModel model = new SpinnerNumberModel(1,1,10,1);
        nsection = new JSpinner(model);
        centerPanel.add(nsection);

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Annulla");

        if(this.operation.toUpperCase().equals("SHOW"))
        {
            JButton showAllButton = new JButton("Show All");
            southPanel.add(showAllButton);
            showAllButton.addActionListener(this);
        }

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
        String filename;
        int section = 0;

        if(e.getSource() instanceof JButton)
        {
            switch (cmd)
            {
                case "OK":
                    section = (Integer) nsection.getValue();

                case "SHOW ALL":
                {
                    filename = name.getText();
                    boolean goback = true;

                    if(filename.equals(""))
                    {
                        JOptionPane.showMessageDialog(this,"Nome del file necessario!","WARNING",JOptionPane.WARNING_MESSAGE);
                        break;
                    }
                    else
                    {
                        switch(operation)
                        {
                            case "CREATE":
                            case "EDIT":
                            case "SHOW":
                            {
                                switch(MainClient.manageDocument(opCode.valueOf(operation), filename, section))
                                {
                                    case ERR_FILE_ALREADY_EXISTS:
                                    {
                                        JOptionPane.showMessageDialog(this,"File già esistente!","WARNING",JOptionPane.WARNING_MESSAGE);
                                        goback = false;
                                        break;
                                    }

                                    case OP_FAIL:
                                    {
                                        JOptionPane.showMessageDialog(this,"Errore generico nella gestione del file","ERROR",JOptionPane.ERROR_MESSAGE);
                                        goback = false;
                                        break;
                                    }

                                    case OP_OK:
                                }
                                break;
                            }
                        }

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
}