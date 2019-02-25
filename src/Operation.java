import java.io.Serializable;

public class Operation implements Serializable
{
    private opCode code;
    private String username;
    private String password;
    private String filename;
    private String owner;
    private int section;

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
