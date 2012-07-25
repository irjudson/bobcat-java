package bobcat.applications;

import bobcat.linear.ILPSolve;
import bobcat.network.Network;
import bobcat.network.NetworkGenerator;
import bobcat.algorithms.Greedy1;
import bobcat.algorithms.Greedy2;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.util.HashMap;

/**
 * Beam Scheduling Code.
 *
 */
public class BeamScheduling {

    static Logger logger = Logger.getLogger("BeamScheduling");

    public static void main(String[] args) {
        HashMap subscribers;
        NetworkGenerator networkGenerator;
        Network network;
        BeamSchedulingOptions options = new BeamSchedulingOptions();
        CmdLineParser parser = new CmdLineParser(options);
        parser.setUsageWidth(80);

        BasicConfigurator.configure();
        logger.setLevel(Level.DEBUG);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            logger.error("Failed to parse command line arguments.");
            logger.error(e.getMessage());
            parser.printUsage(System.err);
            System.exit(1);
        }

        // Handle options that matter
        networkGenerator = Network.getGenerator(options.nodes, options.clients, options.sectors, options.width, options.height, options.seed, options.theta, options.meanq, options.slotLength, options.channels, options.channelProb);
        network = networkGenerator.createCenteredRadialTree();

        network.calculateBeamSets();

        ILPSolve ilpSolve = new ILPSolve(network);
        double ilpThpt = ilpSolve.solve();

        Greedy1 greedy1 = new Greedy1(network);
        double grdyThpt1 = greedy1.solve();


        Greedy2 greedy2 = new Greedy2(network);
        double grdyThpt2 = greedy2.solve();

        //network.draw(1024, 768, "Beam Scheduling Application");

        System.out.println("Seed, Width, Height, Theta, Relays, Subscribers, Slot Length, MeanQ, Channels, ILP, GDY1, GDY2");
        System.out.println(options.seed + ", " + options.width + ", " + options.height + ", " + options.theta + ", "
                + +options.nodes + ", " + options.clients + ", " + options.slotLength + ", " + options.meanq + ", " + options.channels + ", "
                + ilpThpt + ", " + grdyThpt1 + ", " + grdyThpt2);
    }
}
