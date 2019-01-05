import java.io.Serializable;

public class Operation implements Serializable
{
    private opCode code;
    private String username;
    private String password;

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

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
