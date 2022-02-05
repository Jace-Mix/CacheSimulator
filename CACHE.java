public class CACHE
{
    // Replacement Policies
    public final int LRU = 0;
    public final int PLRU = 1;
    public final int OPTIMAL = 2;

    private int set_count, block_offset_count, index_count, block_offset_mask, index_mask;
    // private int tag_count, tag_mask;
    private int REPLACEMENT_POLICY, plru_iterations = 0, plru_mask;

    // Arrays
    private int[] max_LRU_count, plru_bits;
    private int[][] cache, LRU_count;
    private boolean[][] dirty_bits;
    private boolean[][] valid;

    public CACHE(int size, int associativity, int block_size, int REPLACEMENT_POLICY)
    {
        this.REPLACEMENT_POLICY = REPLACEMENT_POLICY;
        set_count = size / (block_size * associativity);
        
        // Creating the address masking
        block_offset_count = 31 - Integer.numberOfLeadingZeros(block_size);
        index_count = 31 - Integer.numberOfLeadingZeros(set_count);
        // tag_count = 32 - index_count - block_offset_count;

        block_offset_mask = (1 << block_offset_count) - 1;
        // index_mask = ((1 << index_count) - 1) << block_offset_count;
        index_mask = ((1 << index_count) - 1);
        // tag_mask = ((1 << tag_count) - 1) << (block_offset_count + index_count);

        // Initialize empty cache and references
        cache = new int[set_count][associativity];
        dirty_bits = new boolean[set_count][associativity];
        valid = new boolean[set_count][associativity];

        if (REPLACEMENT_POLICY == LRU)
        {
            LRU_count = new int[set_count][associativity];
            max_LRU_count = new int[set_count];
        }
        else if (REPLACEMENT_POLICY == PLRU)
        {
            plru_bits = new int[set_count];
            plru_iterations = 31 - Integer.numberOfLeadingZeros(associativity);
            plru_mask = 1 << (associativity - 2);
        }

    }

    public int[][] getCache()
    {
        return cache;
    }

    public boolean[][] getDirtyBits()
    {
        return dirty_bits;
    }

    // returns [Tag, Index, Block]
    public int[] processAddress(int address)
    {
        int[] retval = new int[4];
        int tempAddress = address;

        // Only used for optimal preprocessing
        retval[3] = tempAddress >> block_offset_count;

        retval[2] = tempAddress & block_offset_mask;
        tempAddress >>= block_offset_count;
        retval[1] = tempAddress & index_mask;
        tempAddress >>= index_count;
        retval[0] = tempAddress;

        return retval;
    }

    // True = HIT, False = MISS
    public boolean inCache(int address)
    {
        int[] bits = processAddress(address);
        int tag = bits[0];
        int set = bits[1];
        int[] set_copy = cache[set];
        boolean[] valid_check = valid[set];

        for (int i = 0; i < set_copy.length; i++)
        {
            if (set_copy[i] == tag && valid_check[i])
            {
                // Update counters
                if (REPLACEMENT_POLICY == LRU)
                {
                    max_LRU_count[set]++;
                    LRU_count[set][i] = max_LRU_count[set];
                }
                else if (REPLACEMENT_POLICY == PLRU)
                {
                    int plru_mask_copy = plru_mask;
                    int min = 0;
                    int max = (set_copy.length - 1);
                    int half_of_set = min + (max-min) / 2;
                    int bit_index = 0;
                    for (int j = 0; j < plru_iterations; j++)
                    {
                        if (i <= half_of_set)
                        {
                            // Go left of tree (0)
                            plru_bits[set] &= (((plru_mask_copy << 1) - 1) ^ (plru_mask_copy >> bit_index));
                            bit_index = (2*bit_index)+1;
                            max = half_of_set - 1;
                        }
                        else
                        {
                            // Go right of tree(1)
                            plru_bits[set] |= (plru_mask_copy >> bit_index);
                            bit_index = (2*bit_index)+2;
                            min = half_of_set + 1;
                        }
                        half_of_set = min + (max-min)/2;
                    }
                }
                return true;
            }
        }

        return false;
    }

    // Returns an evicted address if write-back occurs, -1 if no write-back occurs, or -2 if it already exists in cache
    // Returns (evicted_address, dirty_bit)
    public Pair writeToCache(int address, boolean writeInstruction, int cacheLevel)
    {
        int[] bits = processAddress(address);
        int set = bits[1];
        int tag = bits[0];
        int[] set_copy = cache[set];

        // Check if tag exists
        for (int i = 0; i < set_copy.length && writeInstruction; i++)
        {
            if (set_copy[i] == tag && valid[set][i])
            {
                dirty_bits[set][i] = true;

                if (REPLACEMENT_POLICY == LRU)
                {
                    max_LRU_count[set]++;
                    LRU_count[set][i] = max_LRU_count[set];
                }
                else if (REPLACEMENT_POLICY == PLRU)
                {
                    int plru_mask_copy = plru_mask;
                    int min = 0;
                    int max = (set_copy.length - 1);
                    int half_of_set = min + (max-min) / 2;
                    int bit_index = 0;
                    for (int j = 0; j < plru_iterations; j++)
                    {
                        if (i <= half_of_set)
                        {
                            // Go left of tree (0)
                            plru_bits[set] &= (((plru_mask_copy << 1) - 1) ^ (plru_mask_copy >> bit_index));
                            bit_index = (2*bit_index)+1;
                            max = half_of_set - 1;
                        }
                        else
                        {
                            // Go right of tree(1)
                            plru_bits[set] |= (plru_mask_copy >> bit_index);
                            bit_index = (2*bit_index)+2;
                            min = half_of_set + 1;
                        }
                        half_of_set = min + (max-min)/2;
                    }
                }

                return new Pair(address, false, true);
            }
        }

        // Check for invalid line for space
        for (int i = 0; i < valid[set].length; i++)
        {
            if (valid[set][i] == false)
            {
                cache[set][i] = tag;
                valid[set][i] = true;

                if (writeInstruction)
                    dirty_bits[set][i] = true;
                else
                    dirty_bits[set][i] = false;

                if (REPLACEMENT_POLICY == LRU)
                {
                    max_LRU_count[set]++;
                    LRU_count[set][i] = max_LRU_count[set];
                }
                else if (REPLACEMENT_POLICY == PLRU)
                {
                    int plru_mask_copy = plru_mask;
                    int min = 0;
                    int max = (set_copy.length - 1);
                    int half_of_set = min + (max-min) / 2;
                    int bit_index = 0;
                    for (int j = 0; j < plru_iterations; j++)
                    {
                        if (i <= half_of_set)
                        {
                            // Go left of tree (0)
                            plru_bits[set] &= (((plru_mask_copy << 1) - 1) ^ (plru_mask_copy >> bit_index));
                            bit_index = (2*bit_index)+1;
                            max = half_of_set - 1;
                        }
                        else
                        {
                            // Go right of tree(1)
                            plru_bits[set] |= (plru_mask_copy >> bit_index);
                            bit_index = (2*bit_index)+2;
                            min = half_of_set + 1;
                        }
                        half_of_set = min + (max-min)/2;
                    }
                }
                
                return new Pair(address, false, false);
            }
        }

        // If all lines are valid, find LRU to evict
        if (REPLACEMENT_POLICY == LRU)
            return LRU_Replacement(tag, set, writeInstruction);
        else if (REPLACEMENT_POLICY == PLRU)
            return PLRU_Replacement(tag, set, writeInstruction);
        else if (REPLACEMENT_POLICY == OPTIMAL)
            return Optimal_Replacement(address, writeInstruction, cacheLevel);

        return new Pair(address, false, false);
    }

    public Pair LRU_Replacement(int tag, int set, boolean writeInstruction)
    {
        int LRU_location = 0;
        int min_count = 100001;
        for (int i = 0; i < LRU_count[set].length; i++)
        {
            if (LRU_count[set][i] < min_count)
            {
                LRU_location = i;
                min_count = LRU_count[set][i];
            }
        }

        int evicted_address = reconstructAddress(cache[set][LRU_location], set);
        boolean tempDirtyBit = dirty_bits[set][LRU_location];
        cache[set][LRU_location] = tag;
        valid[set][LRU_location] = true;

        if (writeInstruction)
            dirty_bits[set][LRU_location] = true;
        else
            dirty_bits[set][LRU_location] = false;

        max_LRU_count[set]++;
        LRU_count[set][LRU_location] = max_LRU_count[set];

        return new Pair(evicted_address, tempDirtyBit, false);
    }

    public Pair PLRU_Replacement(int tag, int set, boolean writeInstruction)
    {
        int plru_bit_copy = plru_bits[set];
        int plru_mask_copy = plru_mask;
        int index = 0;
        int bit_index = 0;
        for (int i = 0; i < plru_iterations; i++)
        {
            // 0 = Right, 1 = Left
            if ((plru_bit_copy & plru_mask_copy) == 0)
            {
                bit_index = (2*bit_index)+2;
                plru_mask_copy = plru_mask >> bit_index;
            }
            else
            {
                bit_index = (2*bit_index)+1;
                plru_mask_copy = plru_mask >> bit_index;
                index |= 1;
            }
            index <<= 1;
        }
        index >>= 1;
        index = (cache[set].length - index) - 1;

        int evicted_address = reconstructAddress(cache[set][index], set);
        boolean tempDirtyBit = dirty_bits[set][index];
        cache[set][index] = tag;
        valid[set][index] = true;

        if (writeInstruction)
            dirty_bits[set][index] = true;
        else
            dirty_bits[set][index] = false;

        int[] set_copy = cache[set];
        int starting_point = (set_copy.length-1) + index;   // Assumes associativity is a power of 2 for PLRU
        int shift = 0;
        for (int j = 0; j < plru_iterations; j++)
        {
            starting_point = (starting_point-1)/2;
            shift = (set_copy.length-2) - starting_point;
            plru_bits[set] ^= (1 << shift);
        }
    
        return new Pair(evicted_address, tempDirtyBit, false);
    }

    public Pair Optimal_Replacement(int address, boolean writeInstruction, int cacheLevel)
    {
        int[] bits = processAddress(address);
        int tag = bits[0];
        int set = bits[1];
        int[] set_copy = cache[set];
        int tempTagAndSet = 0;
        int max_timestep = -1;
        int timestep = -1;
        int index = -1;

        for (int i = 0; i < set_copy.length; i++)
        {
            tempTagAndSet = (set_copy[i] << index_count) | set;
            if (cacheLevel == 1)
            {
                if (Memory_Hierarchy.optimal_array_L1.get(tempTagAndSet).peek() == null)
                    timestep = 100001;
                else
                    timestep = Memory_Hierarchy.optimal_array_L1.get(tempTagAndSet).element();
            }
            else if (cacheLevel == 2)
            {
                if (Memory_Hierarchy.optimal_array_L2.get(tempTagAndSet).peek() == null)
                    timestep = 100001;
                else
                    timestep = Memory_Hierarchy.optimal_array_L2.get(tempTagAndSet).element();
            }

            if (timestep > max_timestep)
            {
                max_timestep = timestep;
                index = i;
            }
        }
        
        int evicted_address = reconstructAddress(cache[set][index], set);
        boolean tempDirtyBit = dirty_bits[set][index];
        cache[set][index] = tag;
        valid[set][index] = true;

        if (writeInstruction)
            dirty_bits[set][index] = true;
        else
            dirty_bits[set][index] = false;
        
        return new Pair(evicted_address, tempDirtyBit, false);
    }

    public boolean unconditionalEvict(int address)
    {
        int[] bits = processAddress(address);
        int tag = bits[0];
        int set = bits[1];
        int[] set_copy = cache[set];
        boolean dirty_bit_return;

        for (int i = 0; i < set_copy.length; i++)
        {
            if (set_copy[i] == tag)
            {
                dirty_bit_return = dirty_bits[set][i];
                valid[set][i] = false;
                dirty_bits[set][i] = false;
                return dirty_bit_return;
            }
        }

        return false;
    }

    public int reconstructAddress(int tag, int set)
    {
        int retval = 0;
        retval |= tag;
        retval <<= index_count;
        retval |= set;
        retval <<= block_offset_count;
        return retval;
    }

    public int maxHexLength()
    {
        int tag_count = 31 - index_count - block_offset_count;
        int hex_count;
        if (tag_count % 4 == 0)
            hex_count = tag_count / 4;
        else
            hex_count = tag_count / 4 + 1;
        
        return hex_count;
    }
}