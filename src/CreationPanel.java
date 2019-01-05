import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CreationPanel extends JPanel implements ActionListener
{

    private JTextField name;
    private JSpinner nsection;

    public CreationPanel()
    {
        this.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel();
        JPanel centerPanel = new JPanel();
        JPanel southPanel = new JPanel();

        northPanel.add(new JLabel("Inserisci i dati del documento da creare"));

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

        if(e.getSource() instanceof JButton)
        {
            switch (cmd)
            {
                case "OK":
                {
                    MainClient.createDocument(name.getText(), (Integer) nsection.getValue());
                }
                case "ANNULLA":
                {
                    //nascondo il frame di creazione
                    MyFrame old_f = (MyFrame) SwingUtilities.getWindowAncestor(this);
                    old_f.setVisible(false);
                    //mostro il vecchio frame, contenente la chat e i button per le richieste
                    MyFrame f = new MyFrame("turing");
                    break;
                }
            }
        }
    }
}