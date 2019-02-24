import java.io.Serializable;
import java.net.ServerSocket;
import java.util.Date;

public class Invitation implements Serializable
{
    private String sender;
    private String filename;
    private Date date;

    public Invitation(String sender, String filename, Date date)
    {
        this.sender = sender;
        this.filename = filename;
        this.date = date;
    }

    public String getSender() {
        return sender;
    }

    public String getFilename() {
        return filename;
    }

    public Date getDate() {
        return date;
    }
}
