package beamscheduling;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class CmdLineOptions {

    // Execution stuff
  @Option(name = "-w", aliases = {"--width"}, metaVar = "WIDTH",
          usage = "Width of the simulation bounding box.")
    public double width = 20000.0;

  @Option(name = "-h", aliases = {"--height"}, metaVar = "HEIGHT",
          usage = "Height of the simulation bounding box.")
    public double height = 20000.0;

  @Option(name = "-n", aliases = {"--nodes"}, metaVar = "NODES",
          usage = "Number of nodes in the base network.")
    public int nodes = 6;

  @Option(name = "-c", aliases = {"--clients"}, metaVar = "CLIENTS",
          usage = "Number of clients using the network.")
    public int clients = 40;

  @Option(name = "-m", aliases = {"--sectors"}, metaVar = "SECTORS",
          usage = "Number of sectors per antenna / node.")
    public int sectors = 8;

  @Option(name = "-s", aliases = {"--seed"}, metaVar = "SEED",
          usage = "Specify the random seed, defaults to a random seed.")
    public long seed = System.nanoTime();

  @Option(name = "-t", aliases = {"--theta"}, metaVar = "THETA",
          usage = "Specify beam width in degrees for theta.")
    public int theta = 30;
  
  @Option(name = "-u", aliases = {"--meanq"}, metaVar = "MEANQ",
          usage = "Specify the mean queue length for each subscriber.")
    public double meanq = 40000.0;
  
    @Option(name = "-l", aliases = {"--slotlength"}, metaVar = "SLOT",
          usage = "Specify the slot length for each subscriber.")
    public double slotLength = 1.0E-3;
    
  @Option(name = "-k", aliases = {"--channels"}, metaVar = "CHANNELS",
          usage = "Specify the number of channels available at each relay.")
    public int channels = 4;
    
}
