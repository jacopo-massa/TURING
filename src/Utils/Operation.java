package Utils;

import java.io.Serializable;

/**
 * Operation rappresenta la richiesta che il client manda al server,
 * contenente il codice della richiesta da effettuare e altri parametri utili.
 */
public class Operation implements Serializable
{
    private opCode code;        // codice della richiesta da effettuare
    private String username;    // username dell'utente che effettua la richiesta
    private String password;    // password dell'utente che effettua la richiesta
    private String filename;    // nome del file che si vuole editare/visualizzare o per cui si richiede una collaborazione
    private String owner;       // owner del file 'filename'
    private int section;        // numero di sezione del file 'filename'

    public Operation(String username)
    {
        this.username = username;
    }

    public opCode getCode() {
        return code;
    }

    public void setCode(opCode code) {
        this.code = code;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getSection() {
        return section;
    }

    public void setSection(int nsection) {
        this.section = nsection;
    }
}
