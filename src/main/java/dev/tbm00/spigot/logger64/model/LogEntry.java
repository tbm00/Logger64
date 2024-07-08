package dev.tbm00.spigot.logger64.model;

public class LogEntry {

    private int id;
    private String initiatorUUID;
    private String receiverUUID;
    private int rep;

    public LogEntry(int id, String initiatorUUID, String receiverUUID, int rep) {
        this.id = id;
        this.initiatorUUID = initiatorUUID;
        this.receiverUUID = receiverUUID;
        this.rep = rep;
    }

    public LogEntry(String initiatorUUID, String receiverUUID, int rep) {
        this.initiatorUUID = initiatorUUID;
        this.receiverUUID = receiverUUID;
        this.rep = rep;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInitiatorUUID() {
        return initiatorUUID;
    }

    public void setInitiatorUUID(String initiatorUUID) {
        this.initiatorUUID = initiatorUUID;
    }

    public String getReceiverUUID() {
        return receiverUUID;
    }

    public void setReceiverUUID(String receiverUUID) {
        this.receiverUUID = receiverUUID;
    }

    public int getRep() {
        return rep;
    }

    public void setRep(int rep) {
        this.rep = rep;
    }
}
