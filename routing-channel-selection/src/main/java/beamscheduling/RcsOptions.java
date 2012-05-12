package beamscheduling;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class RcsOptions {

    // Execution stuff
  @Option(name = "-w", aliases = {"--width"}, metaVar = "WIDTH",
          usage = "Width of the simulation bounding box (m).")
    public double width = 50000.0;

  @Option(name = "-h", aliases = {"--height"}, metaVar = "HEIGHT",
          usage = "Height of the simulation bounding box (m).")
    public double height = 50000.0;

  @Option(name = "-n", aliases = {"--nodes"}, metaVar = "NODES",
          usage = "Number of nodes in the base network.")
    public int relays = 25;

  @Option(name = "-s", aliases = {"--seed"}, metaVar = "SEED",
          usage = "Specify the random seed, defaults to a random seed.")
    public long seed = System.nanoTime();

  @Option(name = "-u", aliases = {"--users"}, metaVar = "USERS",
          usage = "Number of users in the network")
    public int subscribers = 5;
  
  @Option(name = "-c", aliases = {"--channels"}, metaVar = "CHANNELS",
         usage = "Specify the number of channels available for each frequency.")
    public int channels = 3;

  @Option(name = "-d", aliases = {"--display"}, metaVar = "DISPLAY",
          usage = "Draw the graphs in a window.")
    public Boolean display = false;

  @Option(name = "-v", aliases = {"--verbose"}, metaVar = "VERBOSE",
          usage = "print lots of information.")
    public Boolean verbose = false;

  @Option(name = "-i", aliases = {"--iterations"}, metaVar = "ITER",
          usage = "average over specified number of iterations.")
    public Integer iter = 10;

  @Option(name = "-r", aliases = {"--rcs"}, metaVar = "RCS",
          usage = "Consider the best N paths in RCS calcuations.")
    public Integer consider = 50;
}
