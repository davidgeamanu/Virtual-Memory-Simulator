package ro.tuc.virtualmemorysimulator.BusinessLogic;

import ro.tuc.virtualmemorysimulator.Model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MemoryManager {

    private static final int PAGE_SIZE = 4096; // 4 KB
    private static final int DIR_BITS = 10; // bits 31..22
    private static final int TAB_BITS = 10; // bits 21..12
    private static final int OFF_BITS = 12; // bits 11..0
    private static final int INDEX_MASK = 0x3FF; // 10 bits = 11_1111_1111
    private static final int OFFSET_MASK = 0xFFF; // 12 bits

    private static final int NUM_FRAMES = 32;

    private final PageDirectory directory;
    private final TLB tlb;

    private final List<Frame> frames;
    private final Algorithm algorithm;

    // Input trace: virtual addresses (example: 12345ABC) (HEXADECIMAL)
    private List<Integer> addressTrace = new ArrayList<>();
    private List<Integer> vpnTrace = new ArrayList<>();

    private int totalRequests = 0;
    private int totalFaults = 0;

    private Integer lastEvictedFrame = null;
    private Integer lastVictimVpn = null;

    private String lastAccessType = null;

    public MemoryManager(Algorithm algorithm) {

        //numFrames = NUM_FRAMES;

        this.directory = new PageDirectory();
        this.tlb = new TLB();

        this.frames = new ArrayList<>(NUM_FRAMES);
        for (int i = 0; i < NUM_FRAMES; i++) {
            frames.add(new Frame(i));
        }

        this.algorithm = algorithm;
    }

    public void setAddressTrace(List<Integer> virtualAddresses) {
        this.addressTrace = new ArrayList<>(virtualAddresses);
        this.vpnTrace.clear();
        for (int va : virtualAddresses) {
            int vpn = computeVpn(va);
            vpnTrace.add(vpn);
        }
        resetStatsAndStateForNewRun();
    }

    public boolean step(int index, boolean isWrite) {
        if (index < 0 || index >= addressTrace.size())
            return false;
        int va = addressTrace.get(index);
        processVirtualAddress(va, isWrite, index);
        return true;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getTotalFaults() {
        return totalFaults;
    }

    public double getFaultRate() {
        if (totalRequests == 0) {
            return 0.0;
        }
        else {
            return (totalFaults * 100.0) / totalRequests;
        }
    }

    public List<Frame> getFrames(){
        return frames;
    }

    public PageDirectory getDirectory() {
        return directory;
    }

    public TLB getTlb() {
        return tlb;
    }

    public Integer getLastEvictedFrame() {
        return lastEvictedFrame;
    }

    public Integer getLastVictimVpn() {
        return lastVictimVpn;
    }

    public void clearLastEvictedFrame() {
        lastEvictedFrame = null;
        lastVictimVpn = null;
    }

    private void processVirtualAddress(int virtualAddress, boolean isWrite, int currentIndex) {
        totalRequests++;
        lastAccessType = null;

        // We split the address into indices + offset (10 + 10 + 12) as per the Intel architecture
        int dirIndex = (virtualAddress >>> 22) & INDEX_MASK; // bits 31..22
        int tableIndex = (virtualAddress >>> 12) & INDEX_MASK; // bits 21..12
        int offset =  virtualAddress & OFFSET_MASK; // bits 11..0
        int vpn = (dirIndex << TAB_BITS) | tableIndex;

        // Lookup in TLB
        Integer frameFromTlb = tlb.lookup(vpn);
        if (frameFromTlb != null) {
            // TLB HIT
            lastAccessType = "TLB_HIT";
            if (isWrite) {
                // Mark dirty on page-table entry as well
                int dir = (virtualAddress >>> 22) & INDEX_MASK;
                int tab = (virtualAddress >>> 12) & INDEX_MASK;

                PageTable table = directory.getTable(dir);
                Page pte = table.getEntry(tab);
                if (pte != null) pte.setDirty(true);
            }

            updateLastUsedPage(frameFromTlb);
            return;
        }

        // The page table walk
        PageTable table = directory.getTable(dirIndex);
        if (table == null) {
            table = new PageTable();
            directory.setTable(dirIndex, table);
        }

        Page pte = table.getEntry(tableIndex);
        if (pte == null) {
            pte = new Page(vpn);
            table.setEntry(tableIndex, pte);
        }

        if (pte.isValid()) { //Page table hit
            lastAccessType = "PAGE_TABLE_HIT";
            int frameNum = pte.getFrameNumber();
            tlb.update(vpn, frameNum);
            updateLastUsedPage(frameNum);

        } else { //Page fault
            lastAccessType = "PAGE_FAULT";
            //load page (free frame or replacement)
            handlePageFault(dirIndex, tableIndex, vpn, currentIndex);
        }

        if (isWrite) {
            pte.setDirty(true);
        }
    }

    private void handlePageFault(int dirIndex, int tableIndex, int vpn, int currentIndex) {
        totalFaults++;
        lastEvictedFrame = null;
        lastVictimVpn = null;

        int freeFrameIndex = findFreeFrame();
        if (freeFrameIndex != -1) {
            loadPageIntoFrame(dirIndex, tableIndex, vpn, freeFrameIndex, false);
            return;
        }

        int frameToReplace = algorithm.replacePage(frames, vpnTrace, currentIndex);
        lastEvictedFrame = frameToReplace;

        Page victimPage = frames.get(frameToReplace).getCurrentPage();
        int victimVpn;
        if (victimPage != null)
            victimVpn = victimPage.getPageId();
        else
            victimVpn = -1;

        lastVictimVpn = (victimVpn >= 0) ? victimVpn : null;

        invalidateVictimMapping(victimVpn);

        // Load the new page into the chosen frame
        loadPageIntoFrame(dirIndex, tableIndex, vpn, frameToReplace, true);
    }

    private int findFreeFrame() {
        for (Frame f : frames) {
            if (f.getCurrentPage() == null)
                return f.getFrameId();
        }
        return -1;
    }

    private void loadPageIntoFrame(int dirIndex, int tableIndex, int vpn, int frameIndex, boolean evict) {

        PageTable table = directory.getTable(dirIndex);
        if (table == null) {
            table = new PageTable();
            directory.setTable(dirIndex, table);
        }
        Page page = table.getEntry(tableIndex);
        if (page == null) {
            page = new Page(vpn);
            table.setEntry(tableIndex, page);
        }
        page.setValid(true);
        page.setFrameNumber(frameIndex);
        page.setLastUsedTime(LocalDateTime.now());

        Frame frame = frames.get(frameIndex);
        frame.setCurrentPage(page);

        tlb.update(vpn, frameIndex);

        if (!evict && algorithm instanceof FIFOAlgorithm) {
            ((FIFOAlgorithm) algorithm).addFrame(frameIndex);
        }
    }

    private void invalidateVictimMapping(int victimVpn) {
        if (victimVpn < 0) return;
        int victimDir = (victimVpn >>> TAB_BITS) & INDEX_MASK;
        int victimIdx = victimVpn & INDEX_MASK;

        PageTable vt = directory.getTable(victimDir);
        if (vt != null) {
            Page vPage = vt.getEntry(victimIdx);
            if (vPage != null) {
                vPage.setValid(false);
                vPage.setFrameNumber(-1);
            }
        }
        tlb.remove(victimVpn);
    }

    private void updateLastUsedPage(int frameNum) {
        Frame f = frames.get(frameNum);
        if (f.getCurrentPage() != null) {
            f.getCurrentPage().setLastUsedTime(LocalDateTime.now());
        }
    }

    private int computeVpn(int virtualAddress) {
        int dirIndex = (virtualAddress >>> 22) & INDEX_MASK;
        int tableIndex = (virtualAddress >>> 12) & INDEX_MASK;
        return (dirIndex << TAB_BITS) | tableIndex;
    }

    public int computeVpnPublic(int virtualAddress) {
        return computeVpn(virtualAddress);
    }

    public int getDirectoryIndex(int virtualAddress) {
        return (virtualAddress >>> 22) & 0x3FF;
    }

    public int getTableIndex(int virtualAddress) {
        return (virtualAddress >>> 12) & 0x3FF;
    }

    public int getOffset(int virtualAddress) {
        return virtualAddress & 0xFFF;
    }

    private void resetStatsAndStateForNewRun() {
        totalRequests = 0;
        totalFaults = 0;
        tlb.clear();

        lastEvictedFrame = null;
        lastVictimVpn = null;
        lastAccessType = null;

        for (Frame f : frames) {
            f.setCurrentPage(null);
        }

        for (int i = 0; i < 1024; i++) {
            PageTable t = directory.getTable(i);
            if (t != null) {
                t.reset();
            }
        }
    }

    public String getAlgorithmName() {
        return algorithm.getName();
    }

    public int getNumFrames() {
        return NUM_FRAMES;
    }

    public String getLastAccessType() {
        return lastAccessType;
    }

    public void clearLastAccessType() {
        lastAccessType = null;
    }
}
