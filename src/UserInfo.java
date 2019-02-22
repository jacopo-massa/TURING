import java.util.ArrayList;

public class UserInfo
{
    private String password;
    private int online;
    private ArrayList<String> files;
    private String editingFilename;
    private int editingSection;

    public UserInfo(String password)
    {
        this.password = password;
        this.files = new ArrayList<>();
        this.editingFilename = "";
        this.editingSection = 0;
    }

    public ArrayList<String> getFiles() {
        return files;
    }

    public boolean canEdit(String filename)
    {
        return files.contains(filename);
    }

    public boolean addFile(String filename)
    {
        return files.add(filename);
    }

    public boolean removeFile(String filename)
    {
        return files.remove(filename);
    }


    public String getPassword() {
        return password;
    }

    public boolean isOnline()
    {
        return (online == 1);
    }

    public void setOnline() {
        this.online = 1;
    }

    public void setOffline() {
        this.online = 0;
    }

    public String getEditingFilename() {
        return editingFilename;
    }

    public void setEditingFilename(String editingFilename) {
        this.editingFilename = editingFilename;
    }

    public int getEditingSection() {
        return editingSection;
    }

    public void setEditingSection(int editingSection) {
        this.editingSection = editingSection;
    }
}
