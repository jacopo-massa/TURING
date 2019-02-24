import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CreationPanel extends JPanel implements ActionListener
{

    private JTextField name;
    private JSpinner nsection;
    private String editingFilename;

    CreationPanel()
    {
        this.editingFilename = TuringPanel.editingFilename;
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

        this.add(northPanel,BorderLayout.NORTH);
        this.add(centerPanel,BorderLayout.CENTER);
        this.add(southPanel,BorderLayout.SOUTH);
    }

    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand().toUpperCase();
        String filename;
        int section;

        if(e.getSource() instanceof JButton)
        {
            switch (cmd)
            {
                case "OK":
                {
                    section = (Integer) nsection.getValue();
                    filename = name.getText();
                    boolean goback = true;

                    if(filename.equals(""))
                    {
                        JOptionPane.showMessageDialog(this,"Nome del file necessario!","WARNING",JOptionPane.WARNING_MESSAGE);
                        break;
                    }
                    else
                    {
                        switch(MainClient.createDocument(filename, section))
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
                    }
                    if(!goback)
                        break;
                }

                case "ANNULLA":
                {
                    if(editingFilename == null || editingFilename.equals(""))
                        Utils.showNextFrame(frameCode.TURING,this);
                    else
                        Utils.showNextFrame(frameCode.TURING_EDIT,this);
                    break;
                }
            }
        }
    }
}