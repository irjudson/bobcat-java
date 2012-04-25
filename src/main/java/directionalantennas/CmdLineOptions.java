package simulation;

import java.io.File;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class CmdLineOptions {

    @Option(name = "-s", aliases = {"--seed"}, metaVar = "SEED",
    usage = "Specify the seed.")
    public int seed = 1532;

    @Option(name = "-r", aliases = {"--randomSeed"}, metaVar = "RANDOMSEED",
    usage = "Use a random seed.")
    public boolean randomSeed = false;
    
    @Option(name = "-n", aliases = {"--nodes"}, metaVar = "NODES",
    usage = "Specify the number of nodes.")
    public int nodeNumber = 10;

    @Option(name = "-b", aliases = {"--beams"}, metaVar = "BEAMS",
    usage = "Specify the number of beams.")
    public int beams = 8;
    
    @Option(name = "-l", aliases = {"--sideLength"}, metaVar = "LENGTH",
    usage = "Specify the length of the square side.")
    public double squareSide = 30000.0;

    // Only used in the comparison driver, but at least it's captured.
    @Option(name = "-f", aliases = {"--numberOfNeighbors"}, metaVar = "FRIENDS",
    usage = "Specify the number of neighbors.")
    public int neighborsNumber = 3;

    // Not used yet.
    @Option(name = "-v", aliases = {"--verbose"}, metaVar = "VERBOSE",
    usage = "Run verbosely.")
    public boolean verbose = false;

    // Not used yet.
    @Option(name = "-g", aliases = {"--graphs"}, metaVar = "GRAPHS",
    usage = "Show Graphs.")
    public boolean graphs = false;

    // Not used yet.
    @Option(name = "-o", aliases = {"--optimum"}, metaVar = "OPTIMUM",
    usage = "Include the optimum solution.")
    public boolean optimum = false;

    // Not used yet.
    @Option(name = "-d", aliases = {"--dump-graphs"}, metaVar = "DUMPGRAPHS",
    usage = "Write out graphs to files.")
    public boolean dumpGraphs = false;

    @Option(name = "-p", aliases = {"--print-headers"}, metaVar = "HEADERS",
    usage = "Print headers for output data.")
    public boolean headers = false;
    
    @Option(name = "-i", aliases = {"--ivan"}, metaVar = "IVAN",
    usage = "Run only the MSTPlus algorithm.")
    public boolean mstOnly = false;

    @Option(name = "-O", aliases = {"--optOnly"}, metaVar = "OPTONLY",
    usage = "Run only the optimal algorithm.")
    public boolean optOnly = false;
}
