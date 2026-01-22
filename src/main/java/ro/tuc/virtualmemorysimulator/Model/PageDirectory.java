package ro.tuc.virtualmemorysimulator.Model;

public class PageDirectory {

    private static final int ENTRIES = 1024;

    private final PageTable[] tables;

    public PageDirectory() {
        tables = new PageTable[ENTRIES];
        for (int i = 0; i < ENTRIES; i++) {
            tables[i] = new PageTable();
        }
    }

    public PageTable getTable(int index) {
        if (index < 0 || index >= ENTRIES)
            return null;
        return tables[index];
    }

    public void setTable(int index, PageTable table) {
        if (index < 0 || index >= ENTRIES)
            return;
        tables[index] = table;
    }

    public int size() {
        return ENTRIES;
    }
}

