package dev.tbm00.spigot.logger64.model;

import java.util.Date;

public class PlayerEntry {

    private String playerUUID;
    private String playerUsername;
    private double repAverage;
    private double repAverageLast;
    private int repStaffModifier;
    private double repShown;
    private double repShownLast;
    private int repCount;
    private Date lastLogin;
    private Date lastLogout;

    public PlayerEntry(String playerUUID, String playerUsername,
                double repAverage, double repAverageLast, int repStaffModifier, double repShown, double repShownLast, int repCount,
                Date lastLogin, Date lastLogout) {
        this.playerUUID = playerUUID;
        this.playerUsername = playerUsername;
        this.repAverage = repAverage;
        this.repAverageLast = repAverageLast;
        this.repStaffModifier = repStaffModifier;
        this.repShown = repShown;
        this.repShownLast = repShownLast;
        this.repCount = repCount;
        this.lastLogin = lastLogin;
        this.lastLogout = lastLogout;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(String playerUUID) {
        this.playerUUID = playerUUID;
    }

    public String getPlayerUsername() {
        return playerUsername;
    }

    public void setPlayerUsername(String playerUsername) {
        this.playerUsername = playerUsername;
    }

    public double getRepAverage() {
        return repAverage;
    }

    public void setRepAverage(double repAverage) {
        this.repAverage = repAverage;
    }

    public double getRepAverageLast() {
        return repAverageLast;
    }

    public void setRepAverageLast(double repAverageLast) {
        this.repAverageLast = repAverageLast;
    }

    public int getRepStaffModifier() {
        return repStaffModifier;
    }

    public void setRepStaffModifier(int repStaffModifier) {
        this.repStaffModifier = repStaffModifier;
    }

    public double getRepShown() {
        return repShown;
    }

    public void setRepShown(double repShown) {
        this.repShown = repShown;
    }

    public double getRepShownLast() {
        return repShownLast;
    }

    public void setRepShownLast(double repShownLast) {
        this.repShownLast = repShownLast;
    }

    public int getRepCount() {
        return repCount;
    }

    public void setRepCount(int repCount) {
        this.repCount = repCount;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Date getLastLogout() {
        return lastLogout;
    }

    public void setLastLogout(Date lastLogout) {
        this.lastLogout = lastLogout;
    }
}
