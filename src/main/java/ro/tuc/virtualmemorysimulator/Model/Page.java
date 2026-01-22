package ro.tuc.virtualmemorysimulator.Model;

import java.time.LocalDateTime;

public class Page {
    private final int id;
    private boolean valid;
    private int frameNumber;
    private LocalDateTime lastUsedTime;
    private boolean dirty;

    public Page(int id) {
        this.id = id;
        this.valid = false;
        this.frameNumber = -1;
        this.lastUsedTime = LocalDateTime.now();
        this.dirty = false;
    }

    public int getPageId() {
        return id;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public void setFrameNumber(int frameNumber) {
        this.frameNumber = frameNumber;
    }

    public LocalDateTime getLastUsedTime() {
        return lastUsedTime;
    }

    public void setLastUsedTime(LocalDateTime lastUsedTime) {
        this.lastUsedTime = lastUsedTime;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
