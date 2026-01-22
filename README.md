# Virtual Memory Simulator

A JavaFX-based educational simulator that visualizes how virtual memory management works in modern operating systems. This tool demonstrates the complete address translation process, including TLB lookups, page table walks, and page replacement algorithms.

## Features

- **Interactive Step-by-Step Execution**: Walk through each memory access one step at a time to understand the complete address translation process
- **Multiple Page Replacement Algorithms**: Compare FIFO, LRU, and Optimal algorithms
- **Real-Time Visualization**: Watch the TLB, Page Directory, Page Tables, and Physical Memory update in real-time
- **Address Breakdown Display**: See how 32-bit virtual addresses are split into Directory Index, Table Index, and Offset
- **Statistics Tracking**: Monitor page fault rates and total memory requests
- **Two-Level Page Table**: Simulates Intel x86-style paging with a Page Directory and Page Tables

## Screenshots

The simulator features a modern, card-based UI with:
- Left panel for simulation controls and trace input
- Center panel showing current instruction, address breakdown, and execution status
- Tables displaying Page Directory, Page Table, TLB, and Physical Memory contents
- Real-time statistics in the header

## System Configuration

The simulator models a system with the following characteristics:

| Parameter | Value |
|-----------|-------|
| Address Size | 32-bit (4 GB virtual address space) |
| Page Size | 4096 bytes (4 KB) |
| Offset Bits | 12 |
| Directory Index Bits | 10 (1024 entries) |
| Table Index Bits | 10 (1024 entries per table) |
| VPN Bits | 20 (Directory + Table) |
| Physical Frames | 32 |
| Physical Memory | 128 KB |
| TLB Entries | 16 |

## Requirements

- **Java**: JDK 17 or higher (tested with JDK 23)
- **Maven**: 3.6 or higher
- **JavaFX**: 17.0.6 (automatically downloaded via Maven)

## Building and Running

### Using Maven

```bash
# Clone the repository
git clone https://github.com/yourusername/VirtualMemorySimulator.git
cd VirtualMemorySimulator

# Build and run
mvn clean javafx:run
```

### Using an IDE

1. Import the project as a Maven project
2. Run `ro.tuc.virtualmemorysimulator.PresentationLayer.App`

## Usage Guide

### Getting Started

1. **Start Simulation**: Select a replacement algorithm (FIFO, LRU, or Optimal) and click "Start Simulation"
2. **Enter Trace**: Input memory addresses in hexadecimal format with R/W flags (e.g., `12345678:R, ABCD1234:W`)
3. **Submit Trace**: Click "Submit Trace" to load the addresses
4. **Execute**: Use "Step" to advance one stage at a time, or "Run" to complete the current instruction

### Input Format

Addresses should be in the format: `ADDRESS:OPERATION`

- `ADDRESS`: 32-bit hexadecimal value (e.g., `12345678`, `0000A000`)
- `OPERATION`: `R` for read, `W` for write

Multiple addresses can be separated by commas or whitespace:
```
00000000:R, 00001000:R, 00002000:W
```

### Execution Stages

Each memory access goes through four stages:

1. **Breakdown**: Address is decoded into Directory Index, Table Index, and Offset
2. **TLB Search**: The VPN is looked up in the Translation Lookaside Buffer
3. **TLB Result**: Shows whether it was a TLB hit or miss
4. **Execute**: Performs the actual memory access (may trigger page fault)

## Page Replacement Algorithms

### FIFO (First-In, First-Out)
Replaces the page that has been in memory the longest. Simple but can suffer from Belady's anomaly where increasing frames can increase page faults.

### LRU (Least Recently Used)
Replaces the page that hasn't been accessed for the longest time. Better approximation of optimal but requires tracking access times.

### Optimal (Belady's Algorithm)
Replaces the page that won't be used for the longest time in the future. Theoretical best but requires future knowledge - useful as a benchmark.

## Example Trace Explanation

The built-in example trace demonstrates key virtual memory concepts across 13 phases:

### Phase 1: Preload (Instructions 0-31)
Fills all 32 physical frames with VPNs 0-31. After this phase:
- All frames are occupied
- TLB contains only VPNs 16-31 (last 16 accesses, since TLB size = 16)
- VPNs 0-15 are in memory but NOT in TLB

### Phase 2: TLB Behavior
Demonstrates the difference between TLB hits and page table hits:
- `00005000:R` (VPN 5) - TLB MISS, Page Table HIT (page in memory, not in TLB)
- `0000A000:W` (VPN 10) - TLB MISS, Page Table HIT + sets DIRTY bit
- `00015000:W` (VPN 21) - TLB HIT (still in TLB from preload)

### Phase 3: First Page Fault
`00020000:R` (VPN 32) causes the first page fault. All algorithms agree:
- FIFO: evicts VPN 0 (oldest in queue)
- LRU: evicts VPN 0 (least recently used)
- Optimal: evicts VPN 0 (not used again soon)

### Phases 4-5: Algorithm Divergence
After updating LRU timestamps for specific pages, algorithms start making different decisions:
- FIFO follows its queue order blindly
- LRU considers recent access patterns
- Optimal uses future knowledge

### Phase 6: Belady's Anomaly
`00001000:R` (VPN 1) demonstrates FIFO's weakness:
- FIFO just evicted VPN 1, now needs it again - causes extra page fault
- Optimal kept VPN 1 knowing it would be accessed

### Phases 7-8: Complete Divergence
All three algorithms choose different victim pages, showing their distinct strategies:
- FIFO: purely chronological
- LRU: based on access history
- Optimal: based on future usage

### Phases 9-11: Multiple Page Directories
Accesses pages in different directories (1, 2, 5) to show the two-level page table structure working across the full address space.

### Phases 12-13: Final Verification
Demonstrates the cumulative effects of each algorithm's decisions on overall performance.

## Project Structure

```
VirtualMemorySimulator/
├── src/main/java/ro/tuc/virtualmemorysimulator/
│   ├── BusinessLogic/
│   │   ├── Algorithm.java          # Interface for replacement algorithms
│   │   ├── FIFOAlgorithm.java      # FIFO implementation
│   │   ├── LRUAlgorithm.java       # LRU implementation
│   │   ├── OptimalAlgorithm.java   # Optimal implementation
│   │   └── MemoryManager.java      # Core memory management logic
│   ├── Model/
│   │   ├── Frame.java              # Physical memory frame
│   │   ├── Page.java               # Page table entry
│   │   ├── PageDirectory.java      # First-level page table
│   │   ├── PageTable.java          # Second-level page table
│   │   └── TLB.java                # Translation Lookaside Buffer
│   └── PresentationLayer/
│       ├── App.java                # JavaFX application entry point
│       └── SimulatorController.java # UI controller
├── src/main/resources/ro/tuc/virtualmemorysimulator/
│   ├── SimulatorViewModern.fxml    # Modern UI layout
│   └── simulator-modern.css        # Modern styling
├── pom.xml                         # Maven configuration
└── README.md
```

## Architecture

The project follows a layered architecture:

- **Presentation Layer**: JavaFX UI components and controllers
- **Business Logic**: Memory management algorithms and core simulation logic
- **Model**: Data structures representing memory components

### Key Components

- **MemoryManager**: Coordinates address translation, TLB lookups, page table walks, and page faults
- **PageDirectory**: First-level of the two-level page table (1024 entries)
- **PageTable**: Second-level page tables (1024 entries each)
- **TLB**: 16-entry fully-associative cache using LRU replacement
- **Frame**: Represents a physical memory frame

## Educational Value

This simulator helps understand:

1. **Address Translation**: How virtual addresses map to physical addresses
2. **Two-Level Paging**: Why hierarchical page tables are used
3. **TLB Importance**: The performance impact of TLB hits vs misses
4. **Page Replacement**: Trade-offs between different algorithms
5. **Dirty Bits**: How write operations are tracked for write-back
6. **Page Faults**: What happens when a page isn't in memory

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for:
- Bug fixes
- New replacement algorithms
- UI improvements
- Documentation updates

## License

This project is available for educational purposes.

## Acknowledgments

Developed as an educational tool for understanding operating system memory management concepts.
