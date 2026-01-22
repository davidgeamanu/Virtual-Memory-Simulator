package ro.tuc.virtualmemorysimulator.Model;

public class PageTable {

    private static final int ENTRIES = 1024;

    private final Page[] entries;

    public PageTable() {
        entries = new Page[ENTRIES];
        for (int i = 0; i < ENTRIES; i++) {
            entries[i] = new Page(i);
        }
    }

    public Page getEntry(int index) {
        if (index < 0 || index >= ENTRIES) return null;
        return entries[index];
    }

    public void setEntry(int index, Page entry) {
        if (index < 0 || index >= ENTRIES) return;
        entries[index] = entry;
    }

    public int size() {
        return ENTRIES;
    }

    public boolean hasAnyValidEntry() {
        for (Page e : entries) {
            if (e != null && e.isValid()) {
                return true;
            }
        }
        return false;
    }

    public void reset() {
        for (Page e : entries) {
            if (e != null) {
                e.setValid(false);
                e.setFrameNumber(-1);
                e.setDirty(false);
            }
        }
    }
}
