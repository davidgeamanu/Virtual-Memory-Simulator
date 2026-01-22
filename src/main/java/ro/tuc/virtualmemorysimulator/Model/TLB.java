package ro.tuc.virtualmemorysimulator.Model;

import java.util.LinkedHashMap;
import java.util.Map;

public class TLB {
    private static final int MAX_ENTRIES = 16;//32;
    private final LinkedHashMap<Integer, Integer> cache;

    public TLB() {
        this.cache = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
                return size() > MAX_ENTRIES;
            }
        };
    }

    public Integer lookup(int virtualPageNumber) {
        return cache.get(virtualPageNumber);
    }

    public void update(int virtualPageNumber, int frameNumber) {
        cache.put(virtualPageNumber, frameNumber);
    }

    public void clear() {
        cache.clear();
    }

    public void remove(int vpn) {
        cache.remove(vpn);
    }

    public Map<Integer, Integer> getEntries() {
        return cache;
    }

    public int getMaxEntries() {
        return MAX_ENTRIES;
    }
}