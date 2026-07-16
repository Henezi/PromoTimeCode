package com.kvassuk.promotimecode.data;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String activeCode = null;
    private int playedSeconds = 0;
    private boolean redeemed = false;
    private String activationIp = null;
    private long activationTimestamp = 0;
    private boolean dirty = false;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getActiveCode() {
        return activeCode;
    }

    public void setActiveCode(String activeCode) {
        this.activeCode = activeCode;
        this.dirty = true;
    }

    public int getPlayedSeconds() {
        return playedSeconds;
    }

    public void setPlayedSeconds(int playedSeconds) {
        this.playedSeconds = playedSeconds;
        this.dirty = true;
    }

    public void addPlayedSeconds(int seconds) {
        this.playedSeconds += seconds;
        this.dirty = true;
    }

    public boolean isRedeemed() {
        return redeemed;
    }

    public void setRedeemed(boolean redeemed) {
        this.redeemed = redeemed;
        this.dirty = true;
    }

    public String getActivationIp() {
        return activationIp;
    }

    public void setActivationIp(String activationIp) {
        this.activationIp = activationIp;
        this.dirty = true;
    }

    public long getActivationTimestamp() {
        return activationTimestamp;
    }

    public void setActivationTimestamp(long activationTimestamp) {
        this.activationTimestamp = activationTimestamp;
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

    public int getElapsedMinutes() {
        return playedSeconds / 60;
    }

    public boolean hasActiveCode() {
        return activeCode != null && !activeCode.isEmpty();
    }

    public boolean isEligibleForReward(int requiredMinutes) {
        return playedSeconds >= requiredMinutes * 60 && !redeemed;
    }

    public void reset() {
        this.activeCode = null;
        this.playedSeconds = 0;
        this.redeemed = false;
        this.activationIp = null;
        this.activationTimestamp = 0;
        this.dirty = true;
    }
}