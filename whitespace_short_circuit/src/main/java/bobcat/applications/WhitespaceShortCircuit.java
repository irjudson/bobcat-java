package bobcat.applications;

import bobcat.network.*;
import bobcat.simulation.Draw;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;
import org.apache.commons.collections15.Transformer;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.util.*;

public class WhitespaceShortCircuit {

    static Logger logger = Logger.getLogger("RoutingChannelSelection");

    // public static void dfsPath(Graph network, Vertex src, Vertex dst,
    //                            String prefix, Vector paths,
    //                            ArrayList<Edge> path) {
    //     for (Object o : network.getNeighbors(src)) {
    //         Vertex v = (Vertex) o;
    //         Edge e = (Edge) network.findEdge(src, v);
    //         if (e == null || path.contains(e)) {     // Bad node or edge
    //             continue;
    //         } else if (v == dst) {           // Found the destination
    //             path.add(e);
    //             paths.add(path.clone());
    //             path.remove(e);
    //         } else {                                 // Still looking...
    //             path.add(e);
    //             dfsPath(network, v, dst, prefix + "|", paths, path);
    //             path.remove(e);
    //         }
    //     }
    // }

    public static Boolean inPath(Graph network, List<Edge> path, Vertex v) {
        HashSet nodes = new HashSet();
        for (Object obj : path) {
            Edge e = (Edge) obj;
            Pair<Vertex> ends = network.getEndpoints(e);
            nodes.add((Vertex) ends.getFirst());
            nodes.add((Vertex) ends.getSecond());
        }
        return nodes.contains(v);
    }

    public static void combinations(Vector<Integer> current, HashSet combos) {
        combos.add(current);
        for (Object o : current) {
            Vector<Integer> next = (Vector<Integer>) current.clone();
            next.remove(o);
            if (!next.isEmpty()) {
                combinations((Vector<Integer>) next.clone(), combos);
            }
        }
    }

    public static PathChannelSet rcsPath(Graph network, Vertex src, Vertex dst, int consider) {
            ChannelSelection cs = new ChannelSelection((Network) network);

            // Initialize
            for (Object o : network.getVertices()) {
                Vertex v = (Vertex) o;
                v.rcsPaths = new TreeMap();
                if (v == src) {
                    PathChannelSet p0 = new PathChannelSet();
                    v.rcsPaths.put(0.0d, p0);
                }
            }

            for (int i = 0; i < network.getVertexCount(); i++) {
                // For each edge see if we can extend the existing path/channel sets
                // with the available path/channel sets
                for (Object o : network.getEdges()) {
                    Edge e = (Edge) o;
                    Pair<Vertex> ends = network.getEndpoints(e);
                    Vertex u = (Vertex) ends.getFirst();
                    Vertex v = (Vertex) ends.getSecond();
                    HashSet<Vector<Integer>> chset = new HashSet<Vector<Integer>>();

                    Vector<Integer> cset = new Vector<Integer>();
                    for (int j = 0; j < e.channels.length; j++) {
                        if (e.channels[j] > 0.0d) {
                            cset.add(j);
                        }
                    }
                    combinations(cset, chset);

                    // First direction
                    for (Object c : u.rcsPaths.keySet()) {
                        PathChannelSet opcs = (PathChannelSet) u.rcsPaths.get(c);
                        ArrayList<Edge> opath = opcs.getPath();
                        if (opath.size() == i && !inPath(network, opath, v)) {
                            PathCS opathCS = opcs.pathCS;

                            for (Object chs : chset) {
                                Vector<Integer> channels = (Vector<Integer>) chs;
                                PathChannelSet npcs = new PathChannelSet(opcs);
                                npcs.path.add(e);
                                TreeSet<LinkChannel> nextChannelTS = new TreeSet();
                                for (int k = 0; k < channels.size(); k++) {
                                    nextChannelTS.add(new LinkChannel(npcs.path.size() - 1, channels.elementAt(k)));
                                }
                                npcs.pathCS.selected.add(nextChannelTS);
                                double thpt = cs.evalPathCS(npcs.path, npcs.pathCS);
                                v.rcsPaths.put(thpt, npcs);
                                // If we added one and we're over, take one out
                                if (v.rcsPaths.keySet().size() > consider) {
                                    v.rcsPaths.remove(v.rcsPaths.firstKey());
                                }
                            }
                        }
                    }

                    // Second direction
                    for (Object c : v.rcsPaths.keySet()) {
                        PathChannelSet opcs = (PathChannelSet) v.rcsPaths.get(c);
                        ArrayList<Edge> opath = opcs.getPath();
                        if (opath.size() == i && !inPath(network, opath, u)) {
                            PathCS opathCS = opcs.pathCS;
                            for (Object chs : chset) {
                                Vector<Integer> channels = (Vector<Integer>) chs;
                                PathChannelSet npcs = new PathChannelSet(opcs);
                                npcs.path.add(e);
                                TreeSet<LinkChannel> nextChannelTS = new TreeSet();
                                for (int k = 0; k < channels.size(); k++) {
                                    nextChannelTS.add(new LinkChannel(npcs.path.size() - 1, channels.elementAt(k)));
                                }
                                npcs.pathCS.selected.add(nextChannelTS);
                                double thpt = cs.evalPathCS(npcs.path, npcs.pathCS);
                                u.rcsPaths.put(thpt, npcs);
                                // If we added one and we're over, take one out
                                if (u.rcsPaths.keySet().size() > consider) {
                                    u.rcsPaths.remove(u.rcsPaths.firstKey());
                                }
                            }
                        }
                    }
                }
            }

            if (dst.rcsPaths.size() == 0) {
                System.out.println("Didn't find RCS Path.");
                return (null);
            }

            return (PathChannelSet) dst.rcsPaths.get(dst.rcsPaths.lastKey());
        }

    public static void main(String[] args) {
        NetworkGenerator networkGenerator;
        Network network;
        WhitespaceShortCircuitOptions options = new WhitespaceShortCircuitOptions();
        CmdLineParser parser = new CmdLineParser(options);
        Draw drawing = null;
        ChannelSelection cs = null;
        double rcsThpt;
        double[][] rcsBisectionBandwidth;

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
        if (options.verbose) {
            System.out.println("Random Seed: " + options.seed);
        }
        networkGenerator = Network.getGenerator(options.relays, options.subscribers, options.width, options.height, options.seed, options.channels, options.channelProb);
        network = networkGenerator.create();
        rcsBisectionBandwidth = new double[network.getVertices().size()][network.getVertices().size()];

        Transformer<Edge, Double> wtTransformer = new Transformer<Edge, Double>() {

            public Double transform(Edge e) {
                if (e.capacity > 0.0) {
                    return e.length;
                } else {
                    return Double.MAX_VALUE;
                }
            }
        };

        Transformer<Edge, Double> pTransformer = new Transformer<Edge, Double>() {

            public Double transform(Edge e) {
                return e.bottleNeckWeight();
            }
        };

        // Set all edge and vertex types to 0
        for (Object o : network.getVertices()) {
            Vertex v = (Vertex) o;
            v.type = 0;
        }
        for (Object o : network.getEdges()) {
            Edge e = (Edge) o;
            e.type = 0;
        }

        // Make the drawing
        if (options.display) {
            drawing = new Draw(network, 1024, 768,
                    "Routing and Channel Selection Application");
            drawing.draw();
        }

        // go over all the nxn nodes and find the throughput using rcs
        for (Object o : network.getVertices()) {
            Vertex source = (Vertex) o;
            for (Object p : network.getVertices()) {
                Vertex destination = (Vertex) p;
                if (source != destination) {

                    // Find dmax and dmin
                    double dmin = Double.MAX_VALUE, dmax = Double.MIN_VALUE;
                    for (Object e1 : network.getEdges()) {
                        Pair<Object> ends = network.getEndpoints(e1);
                        Vertex a = (Vertex) ends.getFirst();
                        Vertex b = (Vertex) ends.getSecond();
                        double ad = (a.distanceTo(source) + b.distanceTo(destination)) / 2.0;
                        if (ad < dmin) {
                            dmin = ad;
                        }
                        if (ad > dmax) {
                            dmax = ad;
                        }
                    }

                    // Compute weights for the edges
                    for (Object e1 : network.getEdges()) {
                        Edge e = (Edge) e1;
                        Pair<Object> ends = network.getEndpoints(e1);
                        Vertex a = (Vertex) ends.getFirst();
                        Vertex b = (Vertex) ends.getSecond();
                        double d = (a.distanceTo(source) + b.distanceTo(destination)) / 2.0;
                        e.weight = (1.0 + (dmax - d) / (dmax - dmin)) / 2.0;
                    }

                    // DijkstraShortestPath<Vertex, Edge> dsp = new DijkstraShortestPath(network, wtTransformer, false);
                    // List<Edge> dpath = dsp.getPath(source, destination);
                    // if (dpath.size() == 0) {
                    //     continue;
                    // } else {
                        // if (options.verbose) {
                        //     System.out.println("Dijkstra Path: " + source + " -> " + destination + " : " + dpath.toString());
                        // }

                        try {
                            cs = new ChannelSelection(network);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            System.out.println("S: " + source + " D: " + destination);
                        }

                        // RCS
                        PathChannelSet pcs = rcsPath(network, source, destination, options.consider);
                        List<Edge> rcsPath = pcs.getPath();
                        if (rcsPath == null) {
                            rcsPath = new ArrayList<Edge>();
                        }
                        if (options.verbose) {
                            System.out.println("RCS Path: " + source + " -> " + destination + rcsPath.toString());
                        }
                        if (options.display) {
                            drawing.draw();
                            for(Object e: rcsPath) {
                                ((Edge)e).type = 0;
                            }
                        }

                        cs = new ChannelSelection(network);
                        rcsThpt = cs.evalPathCS(rcsPath, pcs.getPathCS());
                        rcsBisectionBandwidth[source.id][destination.id] = rcsThpt;
                    // }
                }
            }
        }

        if (options.display) {
            drawing = new Draw(network, 1024, 768,
                    "Routing and Channel Selection Application");
            drawing.draw();
        }

        System.out.println("Seed, Width, Height, Nodes, Users, Channels");
        System.out.println(options.seed + ", " + options.width + ", " + options.height + ", " + options.relays + ", " +
                options.subscribers + ", " + options.channels);

        System.out.println("RCS Bisection Bandwidth:");
        for(int i = 0; i < rcsBisectionBandwidth.length; i++) {
            System.out.print(i + " :");
            for(int j = 0; j < rcsBisectionBandwidth[i].length;j++) {
                System.out.printf(" %09.2g", rcsBisectionBandwidth[i][j]);
            }
            System.out.print("\n");
        }
    }
}
