package Server;

public class FileInfo
{
    private String owner;
    private int nsections;
    private boolean[] sections;
    private int counterEditors;
    private String address;


    FileInfo(String owner, int nsections, String address)
    {
        this.owner = owner;
        this.nsections = nsections;
        this.counterEditors = 0;
        this.address = address;
        sections = new boolean[nsections];
        for (boolean b : sections)
        { b = false; }
    }

    String getOwner() {
        return owner;
    }

    void setOwner(String owner) {
        this.owner = owner;
    }

    int getNsections() {
        return nsections;
    }

    void lockSection(int section)
    {
        sections[section] = true;
    }
    
    void unlockSection(int section)
    {
        sections[section] = false;
    }

    boolean isLocked(int section)
    {
        return sections[section];
    }

    void incCounterEditors()
    {
        this.counterEditors++;
    }

    void decCounterEditors()
    {
        this.counterEditors--;
        if(counterEditors == 0)
            MainServer.usedAddresses.remove(address);
    }

    String getAddress() {
        return address;
    }
}
