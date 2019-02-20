import java.util.concurrent.locks.ReentrantLock;

public class FileInfo
{
    private String owner;
    private int nsections;
    private ReentrantLock[] sections;


    public FileInfo(String owner, int nsections)
    {
        this.owner = owner;
        this.nsections = nsections;
        sections = new ReentrantLock[nsections];
    }

    public String getOwner() {
        return owner;
    }

    public int getNsections() {
        return nsections;
    }

    public void lockSection(int section)
    {
        sections[section].tryLock();
    }
    
    public void unlockSection(int section)
    {
        sections[section].unlock();    
    }
    
    public int getNumLockedSections()
    {
        int counter = 0;
        for (ReentrantLock l: sections)
        {
            if(l.isLocked())
                counter++;
        }

        return counter;
    }

    public boolean isLocked(int section)
    {
        return sections[section].isLocked();
    }
}
