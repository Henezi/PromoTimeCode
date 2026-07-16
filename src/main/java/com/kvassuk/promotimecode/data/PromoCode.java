package com.kvassuk.promotimecode.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PromoCode {

    private String code;
    private String author;
    private String groupName;
    private int durationMinutes;
    private double rewardMoney;
    private int requiredPlaytime;
    private List<String> players = new ArrayList<>();
    private double balance = 0.0;
    private int completedPlayers = 0;
    private boolean dirty = false;

    public PromoCode(String code) {
        this.code = code.toUpperCase();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code.toUpperCase();
        this.dirty = true;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
        this.dirty = true;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName.toLowerCase();
        this.dirty = true;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
        this.dirty = true;
    }

    public double getRewardMoney() {
        return rewardMoney;
    }

    public void setRewardMoney(double rewardMoney) {
        this.rewardMoney = rewardMoney;
        this.dirty = true;
    }

    public int getRequiredPlaytime() {
        return requiredPlaytime;
    }

    public void setRequiredPlaytime(int requiredPlaytime) {
        this.requiredPlaytime = requiredPlaytime;
        this.dirty = true;
    }

    public List<String> getPlayers() {
        return players;
    }

    public void setPlayers(List<String> players) {
        this.players = players;
        this.dirty = true;
    }

    public void addPlayer(UUID uuid) {
        if (!players.contains(uuid.toString())) {
            players.add(uuid.toString());
            this.dirty = true;
        }
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
        this.dirty = true;
    }

    public void addBalance(double amount) {
        this.balance += amount;
        this.dirty = true;
    }

    public int getCompletedPlayers() {
        return completedPlayers;
    }

    public void setCompletedPlayers(int completedPlayers) {
        this.completedPlayers = completedPlayers;
        this.dirty = true;
    }

    public void incrementCompletedPlayers() {
        this.completedPlayers++;
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void markClean() {
        this.dirty = false;
    }

    public boolean hasPlayer(UUID uuid) {
        return players.contains(uuid.toString());
    }

    public int getPlayerCount() {
        return players.size();
    }
}