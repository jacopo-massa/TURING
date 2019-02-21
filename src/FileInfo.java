import java.util.concurrent.locks.ReentrantLock;

public class FileInfo
{
    private String owner;
    private int nsections;
    private boolean[] sections;


    public FileInfo(String owner, int nsections)
    {
        this.owner = owner;
        this.nsections = nsections;
        sections = new boolean[nsections];
    }

    public String getOwner() {
        return owner;
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
    
    public int getNumLockedSections()
    {
        int counter = 0;
        for (boolean l: sections)
            if(l) counter++;

        return counter;
    }

    public boolean isLocked(int section)
    {
        return sections[section];
    }
}
