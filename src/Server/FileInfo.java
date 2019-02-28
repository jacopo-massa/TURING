package Server;

/**
 * FileInfo rappresenta le informazioni utili alla gestione di un documento da parte del server TURING
 * (eccetto il filename, salvato a parte in una collezione).
 */
class FileInfo
{
    private String owner;       // creatore e proprietario del documento
    private int nsections;      // numero di sezioni del documento
    private boolean[] sections; // stato di editing delle varie sezioni { TRUE = in editing, FALSE altrimenti }
    private int counterEditors; // numero di sezioni in fase di editing contemporaneamente
    private String address;     // indirizzo di multicast su cui aprire la socket per la chat tra utenti che editano il documento contemporaneamente


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
