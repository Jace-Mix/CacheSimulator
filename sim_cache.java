import java.util.Scanner;
import java.io.File;

// Used for file location
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class sim_cache
{    
    public static void main(String[] args) throws IOException
    {
        if (args.length < 8)
        {
            System.out.println("Error: missing values");
            return;
        }

        int BLOCKSIZE = Integer.parseInt(args[0]);
        int L1_SIZE = Integer.parseInt(args[1]);
        int L1_ASSOC = Integer.parseInt(args[2]);
        int L2_SIZE = Integer.parseInt(args[3]);    // 0 = no L2 cache
        int L2_ASSOC = Integer.parseInt(args[4]);
        int REPLACEMENT_POLICY = Integer.parseInt(args[5]); // 0 = LRU, 1 = PLRU, 2 = Optimal
        int INCLUSION_PROPERTY = Integer.parseInt(args[6]); // 0 = non-inclusive, 1 = inclusive
        List<Path> trace_file = processFileLocation(Paths.get("../../"), args[7]);  // Attempt to find the trace file here

        Memory_Hierarchy sim = new Memory_Hierarchy(BLOCKSIZE, L1_SIZE, L1_ASSOC, L2_SIZE, L2_ASSOC, REPLACEMENT_POLICY, INCLUSION_PROPERTY);
        File file = trace_file.get(0).toFile(); // Instantiate trace file here

        // Preprocess input if replacement policy is 'Optimal'
        if (REPLACEMENT_POLICY == 2)
        {
            try
            {
                Scanner sc = new Scanner(file);
                char instruction;
                String str = "";
                
                for (int i = 0; sc.hasNextLine(); i++)
                {
                    instruction = sc.next().charAt(0);
                    str = sc.next();
                    int address = Integer.parseInt(str, 16);
                    sim.preprocessInput(address, i);
                    sc.nextLine();
                }
                sc.close();
            } catch (Exception e) {
                System.out.println(e);
                return;
            }
        }

        // Main loop
        try
        {
            Scanner sc = new Scanner(file);
            char instruction;
            String str;
            for (int i = 0; sc.hasNextLine(); i++)
            {
                instruction = sc.next().charAt(0);
                str = sc.next();
                if (instruction == 'r')
                {
                    int address = Integer.parseInt(str, 16);
                    sim.processRead(address);
                }
                else if (instruction == 'w')
                {
                    int address = Integer.parseInt(str, 16);
                    sim.processWrite(address);
                }
                sc.nextLine();
            }
            sc.close();
        } catch (Exception e) {
            System.out.println(e);
            return;
        }

        // Printing results
        int[] statistics = sim.generateStatistics();

        System.out.println("===== Simulator configuration =====");
        System.out.println("BLOCKSIZE:             "+args[0]);
        System.out.println("L1_SIZE:               "+args[1]);
        System.out.println("L1_ASSOC:              "+args[2]);
        System.out.println("L2_SIZE:               "+args[3]);
        System.out.println("L2_ASSOC:              "+args[4]);
        System.out.print("REPLACEMENT POLICY:    ");
        if (REPLACEMENT_POLICY == 0)
            System.out.print("LRU\n");
        else if (REPLACEMENT_POLICY == 1)
            System.out.print("Pseudo-LRU\n");
        else if (REPLACEMENT_POLICY == 2)
            System.out.print("Optimal\n");
        System.out.print("INCLUSION PROPERTY:    ");
        if (INCLUSION_PROPERTY == 0)
            System.out.print("non-inclusive\n");
        else if (INCLUSION_PROPERTY == 1)
            System.out.print("inclusive\n");
        System.out.println("trace_file:            "+args[7]);
        System.out.println("===== L1 contents =====");
        sim.printCache(1);
        if (L2_SIZE > 0)
        {
            System.out.println("===== L2 contents =====");
            sim.printCache(2);
        }
        System.out.println("===== Simulation results (raw) =====");
        System.out.println("a. number of L1 reads:        "+String.valueOf(statistics[0]));
        System.out.println("b. number of L1 read misses:  "+String.valueOf(statistics[1]));
        System.out.println("c. number of L1 writes:       "+String.valueOf(statistics[2]));
        System.out.println("d. number of L1 write misses: "+String.valueOf(statistics[3]));
        float miss_rate_L1 = ((float)(statistics[1] + statistics[3]))/(statistics[0] + statistics[2]);
        System.out.printf("e. L1 miss rate:              %.6f\n", miss_rate_L1);
        System.out.println("f. number of L1 writebacks:   "+String.valueOf(statistics[4]));
        System.out.println("g. number of L2 reads:        "+String.valueOf(statistics[5]));
        System.out.println("h. number of L2 read misses:  "+String.valueOf(statistics[6]));
        System.out.println("i. number of L2 writes:       "+String.valueOf(statistics[7]));
        System.out.println("j. number of L2 write misses: "+String.valueOf(statistics[8]));
        if (L2_SIZE > 0)
        {
            float miss_rate_L2 = ((float)statistics[6])/statistics[5];
            System.out.printf("k. L2 miss rate:              %.6f\n", miss_rate_L2);
        }
        else
            System.out.println("k. L2 miss rate:              0");
        System.out.println("l. number of L2 writebacks:   "+String.valueOf(statistics[9]));
        System.out.print("m. total memory traffic:      "+String.valueOf(statistics[10]));
    }

    // Locate file based on "../" location from mp1 directory
    public static List<Path> processFileLocation(Path path, String fileName) throws IOException
    {
        List<Path> result;
        try (Stream<Path> pathStream = Files.find(path,
                    Integer.MAX_VALUE,
                    (p, basicFileAttributes) -> 
                        p.getFileName().toString().equalsIgnoreCase(fileName))
        ) {
            result = pathStream.collect(Collectors.toList());
        }
        return result;
    }
}