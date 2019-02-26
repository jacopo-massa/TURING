package Server;

import Utils.Message;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;

public class UserInfo
{
    private String password;
    private int online;
    private ArrayList<String> files;
    private ArrayList<Message> pendingInvitations;
    private SocketChannel inviteSocketChannel;
    private String editingFilename;
    private int editingSection;

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

    boolean canEdit(String filename)
    {
        return files.contains(filename);
    }

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
        return (online == 1);
    }

    void setOnline() {
        this.online = 1;
    }

    void setOffline() {
        this.online = 0;
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

    void addPendingInvite(Message invitation)
    {
        pendingInvitations.add(invitation);
    }

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
