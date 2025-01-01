import java.util.*;
import java.io.*;

public class Main {

    static class CacheBlock {
        boolean valid;
        boolean dirty;
        int tag;
        int lastUsedTime; // For LRU
        int arrivalTime; // For FIFO

        public CacheBlock() {
            this.valid = false;
            this.dirty = false;
            this.tag = -1;
            this.lastUsedTime = 0;
            this.arrivalTime = 0;
        }
    }

    static class CacheSet {
        CacheBlock[] blocks;

        public CacheSet(int associativity) {
            blocks = new CacheBlock[associativity];
            for (int i = 0; i < associativity; i++) {
                blocks[i] = new CacheBlock();
            }
        }
    }

    static class Cache {
        int blockSize;
        int cacheSize;
        int associativity;
        int numSets;
        CacheSet[] sets;
        int clock; // Simulates time for LRU and FIFO

        public Cache(int cacheSize, int blockSize, int associativity) {
            this.blockSize = blockSize;
            this.cacheSize = cacheSize;
            this.associativity = associativity;
            this.numSets = cacheSize / (blockSize * associativity);
            this.sets = new CacheSet[numSets];

            for (int i = 0; i < numSets; i++) {
                sets[i] = new CacheSet(associativity);
            }
            this.clock = 0;
        }

        public int getSetIndex(int address) {
            return (address / blockSize) % numSets;
        }

        public int getTag(int address) {
            return (address / blockSize) / numSets;
        }
    }

    public static void simulateCache(Cache cache, List<String> instructions, String replacementPolicy) {
        int loadCount = 0, loadMiss = 0;
        int storeCount = 0, storeMiss = 0;
        int totalCycles = 0;

        for (String instruction : instructions) {
            String[] parts = instruction.split(" ");
            String op = parts[0];
            int address = Integer.parseInt(parts[1]);
            cache.clock++;

            int setIndex = cache.getSetIndex(address);
            int tag = cache.getTag(address);
            CacheSet set = cache.sets[setIndex];

            boolean isHit = false;
            int emptyBlockIndex = -1;

            // Check for hit or find empty block
            for (int i = 0; i < cache.associativity; i++) {
                CacheBlock block = set.blocks[i];
                if (block.valid && block.tag == tag) {
                    isHit = true;
                    block.lastUsedTime = cache.clock;
                    break;
                }
                if (!block.valid && emptyBlockIndex == -1) {
                    emptyBlockIndex = i;
                }
            }

            if (isHit) {
                totalCycles += 1; // Cache hit cycle
                if (op.equals("S")) {
                    storeCount++;
                } else {
                    loadCount++;
                }
            } else {
                totalCycles += 100; // Memory access cycle
                if (op.equals("S")) {
                    storeMiss++;
                    storeCount++;
                } else {
                    loadMiss++;
                    loadCount++;
                }

                // Eviction or placement
                if (emptyBlockIndex != -1) {
                    CacheBlock block = set.blocks[emptyBlockIndex];
                    block.valid = true;
                    block.tag = tag;
                    block.lastUsedTime = cache.clock;
                    block.arrivalTime = cache.clock;
                } else {
                    int replaceIndex = getReplacementIndex(set, replacementPolicy);
                    CacheBlock block = set.blocks[replaceIndex];
                    block.tag = tag;
                    block.lastUsedTime = cache.clock;
                    block.arrivalTime = cache.clock;
                }
            }
        }

        System.out.println("Simulation Results:");
        System.out.println("Load Count: " + loadCount);
        System.out.println("Load Miss: " + loadMiss);
        System.out.println("Store Count: " + storeCount);
        System.out.println("Store Miss: " + storeMiss);
        System.out.println("Total Cycles: " + totalCycles);
    }

    private static int getReplacementIndex(CacheSet set, String policy) {
        int index = -1;

        switch (policy) {
            case "LRU":
                int minTime = Integer.MAX_VALUE;
                for (int i = 0; i < set.blocks.length; i++) {
                    if (set.blocks[i].lastUsedTime < minTime) {
                        minTime = set.blocks[i].lastUsedTime;
                        index = i;
                    }
                }
                break;

            case "FIFO":
                int minArrival = Integer.MAX_VALUE;
                for (int i = 0; i < set.blocks.length; i++) {
                    if (set.blocks[i].arrivalTime < minArrival) {
                        minArrival = set.blocks[i].arrivalTime;
                        index = i;
                    }
                }
                break;

            case "Random":
                Random random = new Random();
                index = random.nextInt(set.blocks.length);
                break;
        }

        return index;
    }

    // Helper method for reading user input
    private static int getUserChoice(BufferedReader reader, String message, List<String> options) throws IOException {
        System.out.println(message);
        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i));
        }
        System.out.print("Choose: ");
        int choice = Integer.parseInt(reader.readLine());
        if (choice < 1 || choice > options.size()) {
            throw new IllegalArgumentException("Invalid choice, try again.");
        }
        return choice;
    }

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {
            // Get user choices
            int cacheSize = getCacheSize(reader);
            int blockSize = getBlockSize(reader);
            int associativity = getAssociativity(reader);
            String replacementPolicy = getReplacementPolicy(reader);

            // Input for commands
            List<String> instructions = getInstructions(reader);

            // Create and simulate cache
            Cache cache = new Cache(cacheSize, blockSize, associativity);
            simulateCache(cache, instructions, replacementPolicy);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            reader.close();
        }
    }

    private static int getCacheSize(BufferedReader reader) throws IOException {
        List<String> cacheOptions = Arrays.asList("1024 bytes", "2048 bytes", "4096 bytes");
        int choice = getUserChoice(reader, "Select cache size:", cacheOptions);
        return choice == 1 ? 1024 : choice == 2 ? 2048 : 4096;
    }

    private static int getBlockSize(BufferedReader reader) throws IOException {
        List<String> blockOptions = Arrays.asList("16 bytes", "32 bytes", "64 bytes");
        int choice = getUserChoice(reader, "Select block size:", blockOptions);
        return choice == 1 ? 16 : choice == 2 ? 32 : 64;
    }

    private static int getAssociativity(BufferedReader reader) throws IOException {
        List<String> associativityOptions = Arrays.asList("2-way", "4-way", "8-way");
        int choice = getUserChoice(reader, "Select associativity:", associativityOptions);
        return choice == 1 ? 2 : choice == 2 ? 4 : 8;
    }

    private static String getReplacementPolicy(BufferedReader reader) throws IOException {
        List<String> policyOptions = Arrays.asList("LRU (Least Recently Used)", "FIFO (First In, First Out)", "Random");
        int choice = getUserChoice(reader, "Select replacement policy:", policyOptions);
        return choice == 1 ? "LRU" : choice == 2 ? "FIFO" : "Random";
    }

    private static List<String> getInstructions(BufferedReader reader) throws IOException {
        List<String> instructions = new ArrayList<>();
        System.out.println("\nEnter commands (e.g., L 100, S 200) - type 'exit' to quit:");
        String instruction;
        while (!(instruction = reader.readLine()).equalsIgnoreCase("exit")) {
            instructions.add(instruction);
        }
        return instructions;
    }
}
