package dev.tbm00.spigot.logger64.model;

import java.util.Date;
import java.util.List;

public class PlayerEntry {

    private String username;
    private List<String> knownIPs;
    private String firstIP;
    private String latestIP;
    private Date firstDate;
    private Date latestDate;

    public PlayerEntry(String username, List<String> knownIPs, 
                        String firstIP, String latestIP,
                        Date firstDate, Date latestDate) {
        this.username = username;
        this.knownIPs = knownIPs;
        this.firstIP = firstIP;
        this.latestIP = latestIP;
        this.firstDate = firstDate;
        this.latestDate = latestDate;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getKnownIPs() {
        return knownIPs;
    }

    public String getFirstIP() {
        return firstIP;
    }

    public String getLatestIP() {
        return latestIP;
    }

    public Date getFirstDate() {
        return firstDate;
    }

    public Date getLatestDate() {
        return latestDate;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setKnownIPs(List<String> knownIPs) {
        this.knownIPs = knownIPs;
    }

    public void setFirstIP(String firstIP) {
        this.firstIP = firstIP;
    }

    public void setLatestIP(String latestIP) {
        this.latestIP = latestIP;
    }

    public void setFirstDate(Date firstDate) {
        this.firstDate = firstDate;
    }

    public void setLatestDate(Date latestDate) {
        this.latestDate = latestDate;
    }
}
