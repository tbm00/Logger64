package dev.tbm00.spigot.logger64.model;

import java.util.Date;
import java.util.List;

public class IPEntry {

    private String ip;
    private List<String> knownUsernames;
    private String firstUsername;
    private String latestUsername;
    private Date firstDate;
    private Date latestDate;

    public IPEntry(String ip, List<String> knownUsernames, 
                        String firstUsername, String latestUsername,
                        Date firstDate, Date latestDate) {
        this.ip = ip;
        this.knownUsernames = knownUsernames;
        this.firstUsername = firstUsername;
        this.latestUsername = latestUsername;
        this.firstDate = firstDate;
        this.latestDate = latestDate;
    }

    public String getip() {
        return ip;
    }

    public List<String> getKnownUsernames() {
        return knownUsernames;
    }

    public String getfirstUsername() {
        return firstUsername;
    }

    public String getlatestUsername() {
        return latestUsername;
    }

    public Date getFirstDate() {
        return firstDate;
    }

    public Date getLatestDate() {
        return latestDate;
    }

    public void setip(String ip) {
        this.ip = ip;
    }

    public void setknownUsernames(List<String> knownUsernames) {
        this.knownUsernames = knownUsernames;
    }

    public void setfirstUsername(String firstUsername) {
        this.firstUsername = firstUsername;
    }

    public void setlatestUsername(String latestUsername) {
        this.latestUsername = latestUsername;
    }

    public void setFirstDate(Date firstDate) {
        this.firstDate = firstDate;
    }

    public void setLatestDate(Date latestDate) {
        this.latestDate = latestDate;
    }
}
