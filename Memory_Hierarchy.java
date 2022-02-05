import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class Memory_Hierarchy
{
    // Inclusion Policies
    public final int NON_INCLUSIVE = 0;
    public final int INCLUSIVE = 1;
    public final int OPTIMAL = 2;
    public static HashMap<Integer, Queue<Integer>> optimal_array_L1;
    public static HashMap<Integer, Queue<Integer>> optimal_array_L2;
    
    private int L2_SIZE, INCLUSION_PROPERTY, REPLACEMENT_POLICY, L1_ASSOC, L2_ASSOC;
    private int read_count_L1, read_miss_L1, write_count_L1, write_miss_L1, write_backs_L1;
    private int read_count_L2, read_miss_L2, write_count_L2, write_miss_L2, write_backs_L2, memory_traffic;
    private CACHE L1, L2;

    public Memory_Hierarchy(int BLOCKSIZE, int L1_SIZE, int L1_ASSOC, int L2_SIZE, int L2_ASSOC, int REPLACEMENT_POLICY, int INCLUSION_PROPERTY)
    {
        this.L2_SIZE = L2_SIZE;
        this.INCLUSION_PROPERTY = INCLUSION_PROPERTY;
        this.REPLACEMENT_POLICY = REPLACEMENT_POLICY;
        this.L1_ASSOC = L1_ASSOC;
        this.L2_ASSOC = L2_ASSOC;

        if (L1_SIZE > 0)
            this.L1 = new CACHE(L1_SIZE, L1_ASSOC, BLOCKSIZE, REPLACEMENT_POLICY);
        if (L2_SIZE > 0)
            this.L2 = new CACHE(L2_SIZE, L2_ASSOC, BLOCKSIZE, REPLACEMENT_POLICY);

        if (REPLACEMENT_POLICY == OPTIMAL)
        {
            optimal_array_L1 = new HashMap<>();
            if (L2_SIZE > 0)
                optimal_array_L2 = new HashMap<>();
        }

        read_count_L1 = read_miss_L1 = write_count_L1 = write_miss_L1 = write_backs_L1 = 0;
        read_count_L2 = read_miss_L2 = write_count_L2 = write_miss_L2 = write_backs_L2 = memory_traffic = 0;
    }

    // Only called if replacement policy is 'Optimal'
    public void preprocessInput(int address, int index)
    {
        int[] bits = L1.processAddress(address);
        int tagAndSet = bits[3];

        if (optimal_array_L1.containsKey(tagAndSet))
            optimal_array_L1.get(tagAndSet).add(index);
        else
        {
            optimal_array_L1.put(tagAndSet, new LinkedList<Integer>());
            optimal_array_L1.get(tagAndSet).add(index);
        }

        if (L2_SIZE > 0)
        {
            bits = L2.processAddress(address);
            tagAndSet = bits[3];
            if (optimal_array_L2.containsKey(tagAndSet))
                optimal_array_L2.get(tagAndSet).add(index);
            else
            {
                optimal_array_L2.put(tagAndSet, new LinkedList<Integer>());
                optimal_array_L2.get(tagAndSet).add(index);
            }
        }
    }

    public void processRead(int address)
    {
        read_count_L1++;
        if (!L1.inCache(address))
        {
            read_miss_L1++;
            Pair L1_allocate = L1.writeToCache(address, false, 1);

            if (L1_allocate.dirty_bit == true)
            {
                write_backs_L1++;

                if (L2_SIZE > 0)
                {
                    write_count_L2++;
                    Pair L2_allocate = L2.writeToCache(L1_allocate.address, true, 2);
                    if (L2_allocate.hit == false)
                    {
                        write_miss_L2++;
                        memory_traffic++;
                    }
                    if (L2_allocate.dirty_bit == true)
                    {
                        write_backs_L2++;
                        memory_traffic++;
                    }
                    if (INCLUSION_PROPERTY == INCLUSIVE && L2_allocate.address != L1_allocate.address)
                        if (L1.unconditionalEvict(L2_allocate.address))
                            memory_traffic++;
                }
                else
                {
                    memory_traffic++;
                }
            }

            if (L2_SIZE > 0)
            {
                read_count_L2++;
                if (!L2.inCache(address))
                {
                    read_miss_L2++;
                    memory_traffic++;
                    Pair L2_allocate = L2.writeToCache(address, false, 2);
                    if (L2_allocate.dirty_bit == true)
                    {
                        write_backs_L2++;
                        memory_traffic++;
                    }

                    if (INCLUSION_PROPERTY == INCLUSIVE && L2_allocate.address != address)
                        if (L1.unconditionalEvict(L2_allocate.address))
                            memory_traffic++;
                }
            }
            else
            {
                memory_traffic++;
            }
        }

        if (REPLACEMENT_POLICY == OPTIMAL)
        {
            int[] bits = L1.processAddress(address);
            int tagAndSet = bits[3];
            optimal_array_L1.get(tagAndSet).remove();

            if (L2_SIZE > 0)
            {
                bits = L2.processAddress(address);
                tagAndSet = bits[3];
                optimal_array_L2.get(tagAndSet).remove();
            }
        }
    }

    public void processWrite(int address)
    {
        write_count_L1++;
        Pair L1_write = L1.writeToCache(address, true, 1);
        if (L1_write.hit == false)
        {
            write_miss_L1++;
            if (L1_write.dirty_bit == true)
            {
                write_backs_L1++;

                if (L2_SIZE > 0)
                {
                    write_count_L2++;
                    Pair L2_write = L2.writeToCache(L1_write.address, true, 2);
                    if (L2_write.hit == false)
                    {
                        write_miss_L2++;
                        memory_traffic++;
                    }
                    if (L2_write.dirty_bit == true)
                    {
                        write_backs_L2++;
                        memory_traffic++;
                    }

                    if (INCLUSION_PROPERTY == INCLUSIVE && L2_write.address != L1_write.address)
                        if (L1.unconditionalEvict(L2_write.address))
                            memory_traffic++;
                }
                else
                {
                    memory_traffic++;
                }
            }
            
            if (L2_SIZE > 0)
            {
                read_count_L2++;

                if (!L2.inCache(address))
                {
                    read_miss_L2++;
                    memory_traffic++;
                    Pair L2_allocate = L2.writeToCache(address, false, 2);
                    if (L2_allocate.dirty_bit == true)
                    {
                        write_backs_L2++;
                        memory_traffic++;
                    }

                    if (INCLUSION_PROPERTY == INCLUSIVE && L2_allocate.address != address)
                        if (L1.unconditionalEvict(L2_allocate.address))
                            memory_traffic++;
                }
            }
            else
            {
                memory_traffic++;
            }
        }

        if (REPLACEMENT_POLICY == OPTIMAL)
        {
            int[] bits = L1.processAddress(address);
            int tagAndSet = bits[3];
            optimal_array_L1.get(tagAndSet).remove();

            if (L2_SIZE > 0)
            {
                bits = L2.processAddress(address);
                tagAndSet = bits[3];
                optimal_array_L2.get(tagAndSet).remove();
            }
        }
    }

    // ========== FOR PRINTING PURPOSES BELOW ========== 
    public int[] generateStatistics()
    {
        int[] statistics = {read_count_L1, read_miss_L1, write_count_L1, write_miss_L1, write_backs_L1, 
                            read_count_L2, read_miss_L2, write_count_L2, write_miss_L2, write_backs_L2, memory_traffic};
        return statistics;
    }

    public void printCache(int cache_level)
    {
        int[][] cache;
        boolean[][] dirty_bits;
        int associativity;
        int max_hex_length;
        if (cache_level == 1)
        {
            cache = L1.getCache();
            dirty_bits = L1.getDirtyBits();
            associativity = L1_ASSOC;
            max_hex_length = L1.maxHexLength();
        }
        else
        {
            cache = L2.getCache();
            dirty_bits = L2.getDirtyBits();
            associativity = L2_ASSOC;
            max_hex_length = L2.maxHexLength();
        }

        for (int i = 0; i < cache.length; i++)
        {
            System.out.print("Set     " + i + ":\t");
            for (int j = 0; j < associativity; j++)
            {
                String hex_print = Integer.toHexString(cache[i][j]);
                int remaining_spaces = max_hex_length - hex_print.length();

                System.out.print(hex_print);
                System.out.print(" ");
                for (int k = 0; k < remaining_spaces; k++)
                    System.out.print(" ");

                if (dirty_bits[i][j])
                    System.out.print("D ");
                else
                    System.out.print("  ");
                
                if (cache_level == 1)
                    System.out.print("\t");
            }
            System.out.print("\n");
        }
    }
}