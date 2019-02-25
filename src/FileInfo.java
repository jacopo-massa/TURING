import java.util.ArrayList;

public class FileInfo
{
    private String owner;
    private int nsections;
    private boolean[] sections;
    private int counterEditors;
    private String address;


    public FileInfo(String owner, int nsections, String address)
    {
        this.owner = owner;
        this.nsections = nsections;
        this.counterEditors = 0;
        this.address = address;
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

    public void incCounterEditors()
    {
        this.counterEditors++;
    }

    public void decCounterEditors()
    {
        this.counterEditors--;
        if(counterEditors == 0)
            MainServer.usedAddresses.remove(address);
    }

    public void printStats()
    {
        int i = 1;
        for (boolean b: sections)
        {
            System.out.println(i + " - " + b);
        }
    }

    public String getAddress() {
        return address;
    }
}
