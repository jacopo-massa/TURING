package GUI;

import javax.swing.*;
import java.awt.*;

public class ServerPanel extends JPanel
{
    public static JTextPane logPane;
    public ServerPanel()
    {
        JPanel panel = new JPanel();

        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setPreferredSize(new Dimension(500,700));
        logPane.setMargin(new Insets(5,5,5,5));

        JScrollPane scrollPane = new JScrollPane(logPane);

        panel.add(scrollPane);
        this.add(panel);
    }
}
