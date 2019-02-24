import java.util.ArrayList;

public class FileInfo
{
    private String owner;
    private int nsections;
    private boolean[] sections;
    private int counterEditors;


    public FileInfo(String owner, int nsections)
    {
        this.owner = owner;
        this.nsections = nsections;
        this.counterEditors = 0;
        sections = new boolean[nsections];
        for (boolean b : sections)
        { b = false; }
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getNsections() {
        return nsections;
    }

    public void lockSection(int section)
    {
        sections[section] = true;
    }
    
    public void unlockSection(int section)
    {
        sections[section] = false;
    }


    public boolean isLocked(int section)
    {
        return sections[section];
    }

    public ArrayList<Boolean> getSections()
    {
        ArrayList<Boolean> al = new ArrayList<>(nsections);
        for (int i = 0; i < nsections; i++)
        {
            al.add(i, sections[i]);
        }

        return al;
    }

    public void incCounterEditors()
    {
        this.counterEditors++;
    }

    public void decCounterEditors()
    {
        this.counterEditors--;
    }

    public int getCounterEditors() {
        return counterEditors;
    }
}
