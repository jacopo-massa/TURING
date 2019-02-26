package Utils;

import java.io.Serializable;
import java.util.Date;

public class Message implements Serializable
{
    private String sender;
    private String body;
    private Date date;

    public Message(String sender, String body, Date date)
    {
        this.sender = sender;
        this.body = body;
        this.date = date;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getBody() {
        return body;
    }

    public Date getDate() {
        return date;
    }
}
