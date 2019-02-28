package Utils;

import java.io.Serializable;
import java.util.Date;

/**
 * Message rappresenta gli inviti di collaborazione all'editing
 * o i messaggi mandati sulla chat tra gli utenti che editano lo stesso documento
 */
public class Message implements Serializable
{
    private String sender;  // mittente dell'invito / messaggio
    private String body;    // noem del file al quale si invita / corpo del messaggio
    private Date date;      // data e ora dell'invio dell'invito / messaggio

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
