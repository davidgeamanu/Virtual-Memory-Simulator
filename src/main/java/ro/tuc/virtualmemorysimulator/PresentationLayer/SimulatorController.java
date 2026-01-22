package ro.tuc.virtualmemorysimulator.PresentationLayer;

import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import ro.tuc.virtualmemorysimulator.BusinessLogic.*;
import ro.tuc.virtualmemorysimulator.Model.*;
import ro.tuc.virtualmemorysimulator.Model.Frame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimulatorController {

    // Inputs
    @FXML private ComboBox<String> algorithmChoice;
    @FXML private TextArea traceArea;

    @FXML private Label statusLabel;
    @FXML private Label configLabel;

    // Current / next instruction
    @FXML private Label currentInstructionLabel;
    @FXML private Label nextInstructionsLabel;

    // Address Breakdown
    @FXML private Label dirBitsLabel;
    @FXML private Label tabBitsLabel;
    @FXML private Label offsetBitsLabel;

    @FXML private Label dirHexLabel;
    @FXML private Label tabHexLabel;
    @FXML private Label offsetHexLabel;

    @FXML private Label vpnLabel;

    // Directory Table
    @FXML private TableView<DirectoryRow> directoryTable;
    @FXML private TableColumn<DirectoryRow, String> dirIndexColumn;
    @FXML private TableColumn<DirectoryRow, String> presentColumn;
    @FXML private TableColumn<DirectoryRow, String> tableRefColumn;

    // Page Table
    @FXML private Label pageTableLabel;
    @FXML private TableView<PageTableRow> pageTableTable;
    @FXML private TableColumn<PageTableRow, String> pageIndexColumn;
    @FXML private TableColumn<PageTableRow, String> vpnColumnPT;
    @FXML private TableColumn<PageTableRow, String> frameColumnPT;
    @FXML private TableColumn<PageTableRow, String> validColumn;
    @FXML private TableColumn<PageTableRow, String> dirtyColumn;

    // TLB Table
    @FXML private TableView<TlbRow> tlbTable;
    @FXML private TableColumn<TlbRow, String> vpnColumn;
    @FXML private TableColumn<TlbRow, String> frameColumnTLB;

    // Frame Table
    @FXML private TableView<FrameRow> frameTable;
    @FXML private TableColumn<FrameRow, String> frameIdColumn;
    @FXML private TableColumn<FrameRow, String> vpnInFrameColumn;

    // Statistics
    @FXML private Label statsRequests;
    @FXML private Label statsFaults;
    @FXML private Label statsRate;

    private MemoryManager memoryManager;
    private List<Integer> trace = new ArrayList<>();
    private List<Boolean> isWriteList = new ArrayList<>();
    private int currentIndex = 0;
    private Integer currentVA = null;

    // Step-by-step state machine
    private enum StepStage {
        NONE, // no instruction in progress
        BREAKDOWN, // show dir/table/offset
        TLB_SEARCH, // show "TLB search started"
        TLB_RESULT, // show TLB hit/miss
        EXECUTE // actually call step()
    }

    private StepStage stepStage = StepStage.NONE;

    private static final String[] EXAMPLE_TRACE = new String[]{

            // PHASE 1: PRELOAD (Instructions 0-31) - Fills all 32 frames

            // All in Directory 0, VPNs 0-31
            // After this phase:
            // All 32 frames occupied
            // TLB contains only VPNs 16-31 (last 16 accesses, TLB size = 16!)
            // VPNs 0-15 NOT in TLB but still in frames
            "00000000:R", "00001000:R", "00002000:R", "00003000:R",  // VPN 0-3
            "00004000:R", "00005000:R", "00006000:R", "00007000:R",  // VPN 4-7
            "00008000:R", "00009000:R", "0000A000:R", "0000B000:R",  // VPN 8-11
            "0000C000:R", "0000D000:R", "0000E000:R", "0000F000:R",  // VPN 12-15
            "00010000:R", "00011000:R", "00012000:R", "00013000:R",  // VPN 16-19
            "00014000:R", "00015000:R", "00016000:R", "00017000:R",  // VPN 20-23
            "00018000:R", "00019000:R", "0001A000:R", "0001B000:R",  // VPN 24-27
            "0001C000:R", "0001D000:R", "0001E000:R", "0001F000:R",  // VPN 28-31

            // PHASE 2: TLB Behavior with Size 16

            // Demonstrate the difference between TLB hit and page table hit

            "00005000:R", // VPN 5  - TLB MISS (VPN 0-15 not in TLB), Page Table HIT
            "0000A000:W", // VPN 10 - TLB MISS, Page Table HIT + DIRTY bit set
            "00010000:R", // VPN 16 - TLB MISS (evicted from TLB), Page Table HIT
            "00015000:W", // VPN 21 - TLB HIT + DIRTY bit set

            // PHASE 3: First Page Fault - All Algorithms Agree

            "00020000:R", // VPN 32 (Dir 0, Table 32) - PAGE FAULT!
            // All frames full, must replace
            // FIFO: evicts VPN 0 (oldest in queue)
            // LRU: evicts VPN 0 (least recently used)
            // Optimal: evicts VPN 0 (never used again in future)
            // All three make the same decision here

            // PHASE 4: Setup for Algorithm Divergence
            // Access specific pages to update LRU timestamps
            // LRU will "remember" these accesses, FIFO won't care

            "00002000:R", // VPN 2 - TLB MISS, Page Table HIT, LRU updates timestamp
            "00004000:R", // VPN 4 - TLB MISS, Page Table HIT, LRU updates timestamp
            "00006000:R", // VPN 6 - TLB MISS, Page Table HIT, LRU updates timestamp
            "00008000:R", // VPN 8 - TLB MISS, Page Table HIT, LRU updates timestamp

            // PHASE 5: First Divergence - FIFO vs LRU
            "00021000:R", // VPN 33 (Dir 0, Table 33) - PAGE FAULT!
            // FIFO: evicts VPN 1 (next in circular queue)
            // LRU: evicts VPN 1 (not accessed in Phase 4)
            // Optimal: evicts VPN 32 (won't be used again - just loaded!)
            // Optimal shows "foresight" - evicts the page it just loaded!

            // PHASE 6: FIFO's Belady Anomaly Demonstrated

            "00001000:R", // VPN 1 - Demonstrates FIFO weakness
            // FIFO: PAGE FAULT! Just evicted VPN 1, now needs it back
            //       evicts VPN 2 (this is Belady's anomaly)
            // LRU: PAGE FAULT! VPN 1 also evicted by LRU
            //      evicts VPN 3
            // Optimal: TLB HIT! Kept VPN 1 (knew we'd access it here)

            // PHASE 7: All Three Algorithms Diverge Completely

            "00022000:R", // VPN 34 (Dir 0, Table 34) - PAGE FAULT for all
            // FIFO: evicts VPN 3 (next in queue)
            // LRU: evicts VPN 7 (least recently used)
            // Optimal: evicts VPN 33 (not used again)
            // All three choose different victims!

            // PHASE 8: Show Optimal's Optimal Decision

            "00007000:R", // VPN 7 - Critical demonstration point!
            // FIFO: TLB MISS, Page Table HIT (VPN 7 still in memory)
            // LRU: PAGE FAULT! (evicted VPN 7 in Phase 7)
            //      evicts VPN 9
            // Optimal: TLB MISS, Page Table HIT (kept VPN 7 for this access!)
            // Shows Optimal's "foresight" vs LRU's historical approach

            // PHASE 9-11: Multiple Page Directories

            // Directory 1 (VPN 1024 = 0x400)
            "00400000:R", // VPN 1024 (Dir 1, Table 0) - PAGE FAULT
            "00401000:W", // VPN 1025 (Dir 1, Table 1) - PAGE FAULT + DIRTY

            // Directory 2 (VPN 2048 = 0x800)
            "00800000:R", // VPN 2048 (Dir 2, Table 0) - PAGE FAULT
            "00802000:W", // VPN 2050 (Dir 2, Table 2) - PAGE FAULT + DIRTY

            // Directory 5 (VPN 5120 = 0x1400)
            "01400000:R", // VPN 5120 (Dir 5, Table 0) - PAGE FAULT

            // PHASE 12: Final Verification

            "00003000:R", // VPN 3 - Different outcome for each algorithm
            // FIFO: PAGE FAULT! (evicted in Phase 7)
            // LRU: PAGE FAULT! (evicted in Phase 6)
            // Optimal: TLB MISS, Page Table HIT (kept it!)

            "00009000:W", // VPN 9 - Dirty bit + different states
            // FIFO: PAGE FAULT! (evicted earlier)
            // LRU: PAGE FAULT! (evicted in Phase 8)
            // Optimal: TLB MISS, Page Table HIT + DIRTY

            "00023000:R", // VPN 35 (Dir 0, Table 35) - PAGE FAULT for all

            // PHASE 13: Revisit Other Directories (TLB behavior)
            "00400000:R", // VPN 1024 (Dir 1) - TLB HIT (if in TLB) or Page Table HIT
            "00800000:W", // VPN 2048 (Dir 2) - TLB HIT (if in TLB) or Page Table HIT + DIRTY
    };

    @FXML
    public void initialize() {
        algorithmChoice.getItems().setAll("FIFO", "LRU", "Optimal");
        algorithmChoice.setValue("LRU");
        resetBreakdownLabels();

        initPageDirectoryColumns();
        initPageTableColumns();
        initTlbTableColumns();
        initFrameTableColumns();

        centerColumn(dirIndexColumn);
        centerColumn(presentColumn);
        centerColumn(tableRefColumn);

        centerColumn(pageIndexColumn);
        centerColumn(vpnColumnPT);
        centerColumn(frameColumnPT);
        centerColumn(validColumn);
        centerColumn(dirtyColumn);

        centerColumn(vpnColumn);
        centerColumn(frameColumnTLB);

        centerColumn(frameIdColumn);
        centerColumn(vpnInFrameColumn);

    }

    private void initPageTableColumns() {
        pageIndexColumn.setCellValueFactory(data -> {
            PageTableRow row = data.getValue();
            int idx = row.getIndex();
            String text = idx + " (0x" + String.format("%03X", idx) + ")";
            return new SimpleStringProperty(text);
        });

        vpnColumnPT.setCellValueFactory(data -> {
            PageTableRow row = data.getValue();
            int vpn = row.getVpn();
            String text = vpn + " (0x" + String.format("%05X", vpn) + ")";
            return new SimpleStringProperty(text);
        });

        frameColumnPT.setCellValueFactory(data -> {
            PageTableRow row = data.getValue();
            Page e = row.getEntry();
            int frame = e.getFrameNumber();
            return new SimpleStringProperty(frame >= 0 ? String.valueOf(frame) : "-");
        });

        validColumn.setCellValueFactory(data -> {
            PageTableRow row = data.getValue();
            return new SimpleStringProperty(row.getEntry().isValid() ? "1" : "0");
        });

        dirtyColumn.setCellValueFactory(data -> {
            PageTableRow row = data.getValue();
            return new SimpleStringProperty(row.getEntry().isDirty() ? "1" : "0");
        });

        pageTableTable.getSortOrder().add(validColumn);
        validColumn.setSortType(TableColumn.SortType.DESCENDING);
    }

    private void initPageDirectoryColumns() {
        dirIndexColumn.setCellValueFactory(data -> {
            DirectoryRow row = data.getValue();
            int idx = row.getIndex();
            String text = idx + " (0x" + String.format("%03X", idx) + ")";
            return new SimpleStringProperty(text);
        });

        presentColumn.setCellValueFactory(data -> {
            DirectoryRow row = data.getValue();
            return new SimpleStringProperty(row.isPresent() ? "1" : "0");
        });

        tableRefColumn.setCellValueFactory(data -> {
            DirectoryRow row = data.getValue();
            if (!row.isPresent()) {
                return new SimpleStringProperty("-");
            }
            int idx = row.getIndex();
            return new SimpleStringProperty("PT[" + idx + "]");
        });

        directoryTable.getSortOrder().add(presentColumn);
        presentColumn.setSortType(TableColumn.SortType.DESCENDING);

        directoryTable.setOnMouseClicked(event -> {
            DirectoryRow row = directoryTable.getSelectionModel().getSelectedItem();
            if (row == null) return;

            int dirIndex = row.getIndex();
            updatePageTable(dirIndex);
        });
    }

    private void initTlbTableColumns() {
        vpnColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().vpn));
        frameColumnTLB.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().frame));
    }

    private void initFrameTableColumns() {
        frameIdColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().frameId));
        vpnInFrameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getVpn()));
    }

    @FXML
    private void onStartSimulationClicked() {

        statusLabel.setVisible(true);
        configLabel.setVisible(true);

        final String choice = algorithmChoice.getValue();
        if (choice == null) {
            statusLabel.setText("Please select an algorithm.");
            return;
        }

        final Algorithm algorithm = switch (choice) {
            case "FIFO" -> new FIFOAlgorithm();
            case "LRU" -> new LRUAlgorithm();
            case "Optimal" -> new OptimalAlgorithm();
            default -> null;
        };
        if (algorithm == null) {
            statusLabel.setText("Unknown algorithm selected.");
            return;
        }

        memoryManager = new MemoryManager(algorithm);
        trace = new ArrayList<>();
        isWriteList = new ArrayList<>();
        currentIndex = 0;
        currentVA = null;
        stepStage = StepStage.NONE;

        resetBreakdownLabels();
        directoryTable.getItems().clear();
        pageTableTable.getItems().clear();
        tlbTable.getItems().clear();
        frameTable.getItems().clear();
        updateStats();
        updateCurrentInstructionLabels();
        updateConfigurationInfo();

        statusLabel.setText("Simulation initialized with " + memoryManager.getNumFrames() + " frames. Enter addresses or generate a trace, then press Submit.");
    }

    @FXML
    private void onGenerateRandomTraceClicked() {
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 64; i++) {
            int value = rnd.nextInt();
            boolean isWrite = rnd.nextBoolean();
            String op = isWrite ? "W" : "R";

            if (sb.length() > 0)
                sb.append(", ");
            sb.append(String.format("%08X:%s", value, op));
        }

        traceArea.setText(sb.toString());
        statusLabel.setText("Random trace generated. Press Submit to load it.");
    }

    @FXML
    private void onExampleTraceClicked() {

        statusLabel.setVisible(true);
        if (memoryManager == null) {
            statusLabel.setText("Please start the simulation first.");
            return;
        }


        StringBuilder sb = new StringBuilder();
        for (String s : EXAMPLE_TRACE) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(s);
        }
        traceArea.setText(sb.toString());

        trace.clear();
        isWriteList.clear();

        for (String s : EXAMPLE_TRACE) {
            String[] parts = s.split(":");
            int va = Integer.parseUnsignedInt(parts[0], 16);
            boolean isWrite = parts[1].equals("W");

            trace.add(va);
            isWriteList.add(isWrite);
        }

        // reset memory before preload
        memoryManager.setAddressTrace(trace);

        //first 32 entries are preloaded
        for (int i = 0; i < 32; i++) {
            memoryManager.step(i, isWriteList.get(i));
        }

        currentIndex = 32; //the first 32 pages were processed
        currentVA = trace.get(currentIndex);
        stepStage = StepStage.BREAKDOWN;

        updateDirectoryTable();
        updatePageTable(memoryManager.getDirectoryIndex(trace.get(31)));
        updateTlbTable();
        updateFrameTable();
        updateStats();
        updateCurrentInstructionLabels();
        updateAddressBreakdown(currentVA);

        statusLabel.setText("Example loaded: system preloaded with 32 pages, remaining instructions ready.");
    }

    @FXML
    private void onSubmitTraceClicked() {

        statusLabel.setVisible(true);

        if (memoryManager == null) {
            statusLabel.setText("Please start the simulation first.");
            return;
        }

        trace = parseAddressTrace(traceArea.getText());
        if (trace.isEmpty()) {
            statusLabel.setText("Please enter at least one valid hexadecimal address.");
            return;
        }

        memoryManager.setAddressTrace(trace);
        currentIndex = 0;
        currentVA = trace.get(0);
        stepStage = StepStage.BREAKDOWN;

        resetBreakdownLabels();
        updateCurrentInstructionLabels();
        updateDirectoryTable();
        updatePageTable(memoryManager.getDirectoryIndex(currentVA));
        updateTlbTable();
        updateFrameTable();
        updateStats();

        statusLabel.setText("Trace submitted. Press Step to start processing the current instruction.");
    }

    @FXML
    private void onStepClicked() {
        if (memoryManager == null) {
            statusLabel.setText("Please start the simulation first.");
            return;
        }
        if (trace == null || trace.isEmpty()) {
            statusLabel.setText("Please enter or generate addresses, then press Submit.");
            return;
        }
        if (currentIndex >= trace.size()) {
            statusLabel.setText("All instructions have been processed.");
            stepStage = StepStage.NONE;
            return;
        }

        currentVA = trace.get(currentIndex);
        boolean isWrite = isWriteList.get(currentIndex); // READ or WRITE flag

        switch (stepStage) {

            case NONE, BREAKDOWN -> {
                updateAddressBreakdown(currentVA);
                updateDirectoryTable();
                updatePageTable(memoryManager.getDirectoryIndex(currentVA));
                updateTlbTable();
                updateFrameTable();
                updateStats();

                int dir = memoryManager.getDirectoryIndex(currentVA);
                int tab = memoryManager.getTableIndex(currentVA);
                int vpn = memoryManager.computeVpnPublic(currentVA);

                String op = isWrite ? "WRITE (store)" : "READ (load)";

                statusLabel.setText(
                        "The instruction has been decoded.\n" +
                                "This is a " + op + " instruction.\n" +
                                "VPN is computed as:\n" +
                                "VPN = (dirIndex << 10) | tableIndex\n" +
                                String.format("=> VPN = (%d << 10) | %d = %d (0x%X)",
                                        dir, tab, vpn, vpn) +
                                "\nPress Step to search the VPN in the TLB."
                );
                stepStage = StepStage.TLB_SEARCH;
            }

            case TLB_SEARCH -> {
                int vpn = memoryManager.computeVpnPublic(currentVA);
                statusLabel.setText(
                        String.format(
                                "TLB lookup: VPN %d (0x%X) will be searched in the TLB.\nPress Step to see the result.",
                                vpn, vpn
                        )
                );
                stepStage = StepStage.TLB_RESULT;
            }

            case TLB_RESULT -> {
                int vpn = memoryManager.computeVpnPublic(currentVA);
                Integer frame = memoryManager.getTlb().lookup(vpn);

                if (frame == null) {
                    statusLabel.setText(
                            "TLB Miss. This VPN is not present in the TLB.\n" +
                                    "Press Step to perform the full address translation."
                    );
                } else {
                    statusLabel.setText(
                            String.format(
                                    "TLB Hit! VPN %d (0x%X) → Frame %d.\nPress Step to complete the access.",
                                    vpn, vpn, frame
                            )
                    );
                }

                stepStage = StepStage.EXECUTE;
            }

            case EXECUTE -> {

                boolean wasWrite = isWriteList.get(currentIndex);
                int vpn = memoryManager.computeVpnPublic(currentVA);

                int faultsBefore = memoryManager.getTotalFaults();
                memoryManager.clearLastEvictedFrame();
                memoryManager.clearLastAccessType();

                memoryManager.step(currentIndex, wasWrite);

                int faultsAfter = memoryManager.getTotalFaults();
                boolean pageFaultOccurred = (faultsAfter > faultsBefore);
                Integer evictedFrame = memoryManager.getLastEvictedFrame();
                Integer victimVpn = memoryManager.getLastVictimVpn();
                String accessType = memoryManager.getLastAccessType();

                updateDirectoryTable();
                updatePageTable(memoryManager.getDirectoryIndex(currentVA));
                updateTlbTable();
                updateFrameTable();
                updateStats();

                /*
                StringBuilder sb = new StringBuilder("Instruction processed.\n");

                if (pageFaultOccurred) {
                    sb.append("PAGE FAULT occurred!\n");

                    if (evictedFrame == null) {
                        sb.append("Loaded into a FREE frame (no victim was evicted).\n");
                    } else {
                        sb.append(String.format(
                                "Replaced victim page: VPN %d (0x%05X) from Frame %d.\n",
                                victimVpn, victimVpn, evictedFrame
                        ));
                    }
                }

                if (wasWrite) {
                    sb.append(String.format("Page %d (0x%05X) marked DIRTY.\n", vpn, vpn));
                }
                */

                StringBuilder sb = new StringBuilder();

                if ("TLB_HIT".equals(accessType)) {
                    //sb.append("✓ TLB HIT!\n");
                    sb.append(String.format("VPN %d (0x%05X) found in TLB → Direct access to frame.\n", vpn, vpn));
                    sb.append("No page table walk required. Fast path!\n");

                } else if ("PAGE_TABLE_HIT".equals(accessType)) {
                    sb.append("TLB MISS, but PAGE TABLE HIT!\n");
                    sb.append(String.format("VPN %d (0x%05X) was not in TLB, but found in page table.\n", vpn, vpn));
                    sb.append("Performed page table walk (slower than TLB hit).\n");
                    //sb.append("TLB has been updated with this entry.\n");

                } else if (pageFaultOccurred) {
                    sb.append("PAGE FAULT!\n");
                    sb.append(String.format("VPN %d (0x%05X) not in TLB and not valid in page table.\n", vpn, vpn));

                    if (evictedFrame == null) {
                        sb.append("Loaded into a FREE frame (no victim was evicted).\n");
                    } else {
                        sb.append(String.format(
                                "Victim page replaced: VPN %d (0x%05X) from Frame %d.\n",
                                victimVpn, victimVpn, evictedFrame
                        ));
                    }
                }

                // Add dirty bit information
                if (wasWrite) {
                    sb.append(String.format("\nWRITE operation: VPN %d (0x%05X) marked DIRTY.\n", vpn, vpn));
                }
                sb.append("Page tables, frames, and TLB updated.");

                statusLabel.setText(sb.toString());

                currentIndex++;
                if (currentIndex < trace.size()) {
                    updateCurrentInstructionLabels();
                    updateAddressBreakdown(trace.get(currentIndex));
                    stepStage = StepStage.BREAKDOWN;
                } else {
                    updateCurrentInstructionLabels();
                    resetBreakdownLabels();
                    stepStage = StepStage.NONE;
                    statusLabel.setText(sb + "\nSimulation complete. All instructions have been processed.");
                }
            }
        }
    }

    @FXML
    private void onRunClicked() {
        if (memoryManager == null) {
            statusLabel.setText("Please start the simulation first.");
            return;
        }
        if (trace == null || trace.isEmpty()) {
            statusLabel.setText("Please enter or generate addresses, then press Submit.");
            return;
        }
        if (currentIndex >= trace.size()) {
            statusLabel.setText("All instructions have been processed.");
            return;
        }

        int startingIndex = currentIndex;
        int maxStepsForInstruction = 4; // BREAKDOWN, TLB_SEARCH, TLB_RESULT, EXECUTE

        for (int i = 0; i < maxStepsForInstruction; i++) {
            if (currentIndex != startingIndex) break; // instruction finished (index incremented)
            onStepClicked();
            if (stepStage == StepStage.NONE) break;  // simulation ended
        }
    }

    @FXML
    private void onResetClicked() {
        if (memoryManager == null) {
            statusLabel.setText("Nothing to reset. Start the simulation first.");
            return;
        }

        if (trace == null) {
            trace = new ArrayList<>();
        }

        memoryManager.setAddressTrace(trace); // resets internal state
        currentIndex = 0;
        currentVA = trace.isEmpty() ? null : trace.get(0);
        stepStage = trace.isEmpty() ? StepStage.NONE : StepStage.BREAKDOWN;

        resetBreakdownLabels();
        updateDirectoryTable();
        if (currentVA != null) {
            updatePageTable(memoryManager.getDirectoryIndex(currentVA));
        } else {
            pageTableTable.getItems().clear();
        }
        updateTlbTable();
        updateFrameTable();
        updateStats();
        updateCurrentInstructionLabels();

        if (trace.isEmpty()) {
            statusLabel.setText("Simulation reset. Enter addresses and press Submit.");
        } else {
            statusLabel.setText("Simulation reset. Press Step to start from the first instruction.");
        }
    }

    private List<Integer> parseAddressTrace(String input) {
        List<Integer> result = new ArrayList<>();
        isWriteList.clear();

        if (input == null)
            return result;

        String[] tokens = input.split("[,\\s]+");
        for (String token : tokens) {
            if (token.isBlank()) continue;

            String[] parts = token.split(":");
            if (parts.length != 2) continue;

            try {
                long value = Long.parseLong(parts[0], 16);
                int va = (int)(value & 0xFFFFFFFFL);
                result.add(va);
            } catch (NumberFormatException ignore) {
                continue;
            }

            boolean isWrite = parts[1].equalsIgnoreCase("W");
            isWriteList.add(isWrite);
        }

        return result;
    }

    private void updateAddressBreakdown(int virtualAddress) {
        int dir = (virtualAddress >>> 22) & 0x3FF;
        int tab = (virtualAddress >>> 12) & 0x3FF;
        int off = virtualAddress & 0xFFF;

        // binary
        dirBitsLabel.setText(String.format("%10s", Integer.toBinaryString(dir)).replace(' ', '0'));
        tabBitsLabel.setText(String.format("%10s", Integer.toBinaryString(tab)).replace(' ', '0'));
        offsetBitsLabel.setText(String.format("%12s", Integer.toBinaryString(off)).replace(' ', '0'));

        // hex
        dirHexLabel.setText(String.format("0x%03X", dir)); // up to 0x3FF
        tabHexLabel.setText(String.format("0x%03X", tab)); // up to 0x3FF
        offsetHexLabel.setText(String.format("0x%03X", off)); // up to 0xFFF

        int vpn = memoryManager.computeVpnPublic(virtualAddress);
        vpnLabel.setText(String.format("VPN: %d (0x%05X)", vpn, vpn));
    }

    private void resetBreakdownLabels() {
        dirBitsLabel.setText("[none]");
        tabBitsLabel.setText("[none]");
        offsetBitsLabel.setText("[none]");

        dirHexLabel.setText("[none]");
        tabHexLabel.setText("[none]");
        offsetHexLabel.setText("[none]");

        vpnLabel.setText("VPN: [none]");
    }

    private void updateDirectoryTable() {
        if (memoryManager == null) return;

        directoryTable.getItems().clear();
        PageDirectory dir = memoryManager.getDirectory();

        for (int i = 0; i < 1024; i++) {
            PageTable table = dir.getTable(i);

            // "present" = the page table has at least one valid entry
            boolean present = (table != null && table.hasAnyValidEntry());

            DirectoryRow row = new DirectoryRow(i, present, table);
            directoryTable.getItems().add(row);
        }

        directoryTable.getSortOrder().add(presentColumn);
        presentColumn.setSortType(TableColumn.SortType.DESCENDING);
    }

    private void updatePageTable(int dirIndex) {
        if (memoryManager == null)
            return;

        pageTableLabel.setText("Page Table [" + dirIndex + "]");

        pageTableTable.getItems().clear();

        PageDirectory dir = memoryManager.getDirectory();
        PageTable table = dir.getTable(dirIndex);
        if (table == null)
            return;

        for (int i = 0; i < 1024; i++) {
            Page entry = table.getEntry(i);
            if (entry == null)
                continue;

            PageTableRow row = new PageTableRow(i, entry, dirIndex);
            pageTableTable.getItems().add(row);
        }

        pageTableTable.getSortOrder().add(validColumn);
        validColumn.setSortType(TableColumn.SortType.DESCENDING);
    }

    private void updateTlbTable() {
        if (memoryManager == null)
            return;
        tlbTable.getItems().clear();

        memoryManager.getTlb().getEntries().forEach((vpn, frame) ->
                tlbTable.getItems().add(
                        new TlbRow(String.format("%d (0x%X)", vpn, vpn), String.valueOf(frame))
                ));
    }

    private void updateFrameTable() {
        if (memoryManager == null)
            return;
        frameTable.getItems().clear();

        for (Frame f : memoryManager.getFrames()) {
            Integer vpnValue = (f.getCurrentPage() != null)
                    ? f.getCurrentPage().getPageId()
                    : null;

            frameTable.getItems().add(new FrameRow(f.getFrameId(), vpnValue));
        }

    }

    private void updateStats() {
        if (memoryManager == null) {
            statsRequests.setText("Requests: 0");
            statsFaults.setText("Faults: 0");
            statsRate.setText("Fault Rate: 0%");
            return;
        }
        statsRequests.setText("Requests: " + memoryManager.getTotalRequests());
        statsFaults.setText("Faults: " + memoryManager.getTotalFaults());
        statsRate.setText(String.format("Fault Rate: %.2f%%", memoryManager.getFaultRate()));
    }

    private void updateCurrentInstructionLabels() {
        if (trace == null || trace.isEmpty() || currentIndex >= trace.size()) {
            currentInstructionLabel.setText("Current Instruction: [none]");
            nextInstructionsLabel.setText("Next Instructions: [none]");
            return;
        }

        int va = trace.get(currentIndex);
        boolean isWrite = isWriteList.get(currentIndex);
        String op = isWrite ? "(Write)" : "(Read)";
        currentInstructionLabel.setText(String.format("Current Instruction: %08X %s", va, op));

        StringBuilder sb = new StringBuilder();
        for (int i = currentIndex + 1; i < trace.size(); i++) {
            if (sb.length() > 0) sb.append(", ");

            int vaNext = trace.get(i);
            boolean nextWrite = isWriteList.get(i);
            String opNext = nextWrite ? "(W)" : "(R)";

            sb.append(String.format("%08X %s", vaNext, opNext));
        }

        nextInstructionsLabel.setText("Next Instructions: " + (sb.isEmpty() ? "[none]" : sb));
    }

    private void updateConfigurationInfo() {
        String text = String.format("""
        • Address Size: 32-bit (4 GB virtual space)
        • Page Size: 4096 bytes (4 KB)
        • Offset Bits: 12
        • Page Table Entries: 1024
        • Directory Entries: 1024
        • VPN Bits: 20 (10 + 10)
        • Physical Frames: %d
        • Physical Memory Size: %d KB
        • TLB Entries: %d
        • Page Replacement Algorithm: %s
        """,
                memoryManager.getNumFrames(),
                memoryManager.getNumFrames() * 4, // n frames * 4 KB
                memoryManager.getTlb().getMaxEntries(),
                (memoryManager != null ? memoryManager.getAlgorithmName() : "[none]")
        );

        configLabel.setText(text);
    }

    public static class DirectoryRow {
        private final int index;
        private final boolean present;
        private final PageTable table;

        public DirectoryRow(int index, boolean present, PageTable table) {
            this.index = index;
            this.present = present;
            this.table = table;
        }

        public int getIndex() { return index; }
        public boolean isPresent() { return present; }
    }

    public static class PageTableRow {
        private final int index; // page table index (0–1023)
        private final Page entry;
        private final int dirIndex;

        public PageTableRow(int index, Page entry, int dirIndex) {
            this.index = index;
            this.entry = entry;
            this.dirIndex = dirIndex;
        }

        public int getIndex() { return index; }
        public Page getEntry() { return entry; }

        public int getVpn() {
            return (dirIndex << 10) | index; // VPN = (DIR << 10) | TABLE
        }
    }

    public static class TlbRow {
        final String vpn;
        final String frame;
        String flashColor = null;

        public TlbRow(String vpn, String frame) {
            this.vpn = vpn;
            this.frame = frame;
        }
    }

    public static class FrameRow {
        final String frameId;
        final String vpn;

        public FrameRow(int frameId, Integer vpnValue) {
            this.frameId = String.valueOf(frameId);

            if (vpnValue == null || vpnValue < 0) {
                this.vpn = "(empty)";
            } else {
                this.vpn = vpnValue + " (0x" + String.format("%05X", vpnValue) + ")";
            }
        }

        public String getVpn() { return vpn; }
    }

    private <T> void centerColumn(TableColumn<T, ?> column) {
        column.setStyle("-fx-alignment: CENTER;");
    }

}
