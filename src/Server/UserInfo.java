package Server;

import Utils.Message;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;

/**
 * UserInfo rappresenta le informazioni utili riguardanti un utente
 * (eccetto l'username, salvato a parte in una collezione).
 */
class UserInfo
{
    private String password;    // password dell'utente
    private boolean online;     // stato dell'utente (online / offline)
    private ArrayList<String> files;    // lista dei filename dei documenti a cui può accedere l'utente
    private ArrayList<Message> pendingInvitations;  // lista di inviti ricevuti mentre l'utente era offline
    private SocketChannel inviteSocketChannel;      // riferimento al socket usato per ricevere gli inviti in tempo reale
    private String editingFilename;     // nome del documento che l'utente sta editando ("" altrimenti)
    private int editingSection;         // numero della sezione del documento che l'utente sta editando (0 altrimenti)

    UserInfo(String password)
    {
        this.password = password;
        this.files = new ArrayList<>();
        this.pendingInvitations = new ArrayList<>();
        this.editingFilename = "";
        this.editingSection = 0;
    }

    ArrayList<String> getFiles() {
        return files;
    }

    /**
     * Funzione che conferma se un utente ha i permessi di accedere ad un documento.
     *
     * @param filename nome del documento a cui accedere
     *
     * @return true se l'utente può accedere al documento,
     *         false altrimenti
     */
    boolean canEdit(String filename)
    {
        return files.contains(filename);
    }

    /**
     * Procedura che aggiunge un documento tra quelli a cui l'utente può accedere.
     *
     * @param filename nome del documento da aggiungere
     */
    void addFile(String filename)
    {
        if(!files.contains(filename))
            files.add(filename);
    }

    String getPassword() {
        return password;
    }

    boolean isOnline()
    {
        return online;
    }

    void setOnline() {
        this.online = true;
    }

    void setOffline() {
        this.online = false;
    }

    String getEditingFilename() {
        return editingFilename;
    }

    void setEditingFilename(String editingFilename) {
        this.editingFilename = editingFilename;
    }

    int getEditingSection() {
        return editingSection;
    }

    void setEditingSection(int editingSection) {
        this.editingSection = editingSection;
    }

    ArrayList<Message> getPendingInvitations() {
        return pendingInvitations;
    }

    /**
     * Procedura che aggiunge un invito tra quelli pendenti dell'utente.
     *
     * @param invitation invito da aggiungere
     */
    void addPendingInvite(Message invitation)
    {
        pendingInvitations.add(invitation);
    }

    /**
     * Procedura che svuota la lista di eventi pendenti.
     */
    void clearPendingInvites()
    {
        pendingInvitations.clear();
    }

    SocketChannel getInviteSocketChannel() {
        return inviteSocketChannel;
    }

    void setInviteSocketChannel(SocketChannel inviteSocketChannel) {
        this.inviteSocketChannel = inviteSocketChannel;
    }
}
