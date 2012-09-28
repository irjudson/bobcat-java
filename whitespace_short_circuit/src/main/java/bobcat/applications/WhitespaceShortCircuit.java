package bobcat.applications;

import bobcat.network.*;
import bobcat.simulation.Draw;
import edu.uci.ics.jung.algorithms.shortestpath.PrimMinimumSpanningTree;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;
import org.apache.commons.collections15.Transformer;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import ilog.concert.*;
import ilog.cplex.*;

import java.util.*;

public class WhitespaceShortCircuit {

    static Logger logger = Logger.getLogger("RoutingChannelSelection");

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

    public static int findMaxClique(HashSet channel_cliques, int edge) {
        int max = 0;
        for(Object o: channel_cliques) {
            HashSet clique = (HashSet)o;
            if (clique.contains(edge) && clique.size() > max) {
                max = clique.size();
            }
        }
        return(max);
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
        PrimMinimumSpanningTree psp = null;
        Graph primTree = null;

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

        int count = 0;
        do {
            networkGenerator = Network.getGenerator(options.relays, options.subscribers, options.width, options.height, options.seed+count, options.channels, options.channelProb);
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

            psp = new PrimMinimumSpanningTree(networkGenerator.networkFactory, pTransformer);
            primTree = psp.transform(network);
            count++;
        } while (network.getVertexCount() != primTree.getVertexCount());

        // Handle options that matter
        if (options.verbose) {
            System.out.println("Random Seed: " + options.seed);
        }

        if (options.verbose) {
            System.out.println("Prim Tree: " + primTree.toString());
        }

        for (Object e : primTree.getEdges()) {
            // Color the MST
            ((Edge) e).type = 2;
        }

        Collection<Edge> primEdges = primTree.getEdges();
        Vector toRemove = new Vector();
        for (Object e: network.getEdges()) {
            if (!primEdges.contains(e)) {
                toRemove.addElement(e);
            }
        }

        for(Object e: toRemove) {
            network.removeEdge((Edge)e);
        }

        // go over all the nxn nodes and find the throughput using rcs
        for (Object o : network.getVertices()) {
            Vertex source = (Vertex) o;
            for (Object p : network.getVertices()) {
                Vertex destination = (Vertex) p;
                if (source.id < destination.id) {
                    DijkstraShortestPath<Vertex,Edge> dspath = new DijkstraShortestPath(primTree);
                    List<Edge> spath = dspath.getPath(source, destination);
                    System.out.println(source + " -> " + destination + " : " + spath);

                    // // Find dmax and dmin
                    // double dmin = Double.MAX_VALUE, dmax = Double.MIN_VALUE;
                    // for (Object e1 : network.getEdges()) {
                    //     Pair<Object> ends = network.getEndpoints(e1);
                    //     Vertex a = (Vertex) ends.getFirst();
                    //     Vertex b = (Vertex) ends.getSecond();
                    //     double ad = (a.distanceTo(source) + b.distanceTo(destination)) / 2.0;
                    //     if (ad < dmin) {
                    //         dmin = ad;
                    //     }
                    //     if (ad > dmax) {
                    //         dmax = ad;
                    //     }
                    // }

                    // // Compute weights for the edges
                    // for (Object e1 : network.getEdges()) {
                    //     Edge e = (Edge) e1;
                    //     Pair<Object> ends = network.getEndpoints(e1);
                    //     Vertex a = (Vertex) ends.getFirst();
                    //     Vertex b = (Vertex) ends.getSecond();
                    //     double d = (a.distanceTo(source) + b.distanceTo(destination)) / 2.0;
                    //     e.weight = (1.0 + (dmax - d) / (dmax - dmin)) / 2.0;
                    // }

                    // try {
                    //     cs = new ChannelSelection(network);
                    // } catch (ArrayIndexOutOfBoundsException e) {
                    //     System.out.println("S: " + source + " D: " + destination);
                    // }

                    // // RCS
                    // PathChannelSet pcs = rcsPath(network, source, destination, options.consider);
                    // List<Edge> rcsPath = pcs.getPath();
                    // if (rcsPath == null) {
                    //     rcsPath = new ArrayList<Edge>();
                    // }
                    // if (options.verbose) {
                    //     System.out.println("RCS Path: " + source + " -> " + destination + rcsPath.toString());
                    // }
                    // if (options.display) {
                    //     drawing.draw();
                    //     for(Object e: rcsPath) {
                    //         ((Edge)e).type = 0;
                    //     }
                    // }

                    // cs = new ChannelSelection(network);
                    // rcsThpt = cs.evalPathCS(rcsPath, pcs.getPathCS());
                    // rcsBisectionBandwidth[source.id][destination.id] = rcsThpt;
                }
            }
        }

        // Print out the edge list
        System.out.println(network.getEdgeCount() + " Edges");
        for(Object e: network.getEdges()) {
            System.out.println("\t"+(Edge)e);
        }

        // Print out the conflict graph
        // for(int i = 0; i < network.getEdgeCount() + 1; i++) {
        //     for(int j = 0; j < network.getEdgeCount() + 1; j++) {
        //         for (int k = 0; k < network.numChannels * 3 + 1; k++) {
        //             System.out.println("("+i+","+j+","+k+") = "+network.interferes[i][j][k]);
        //         }
        //     }
        // }

        // Initialize a hash of cliques by channel, the value is a set so we don't get duplicates
        HashMap clique_list = new HashMap();
        for(int k = 0; k < network.numChannels * 3 + 1; k++) {
            clique_list.put(k, new HashSet());
        }
        // Enumerate cliques - There are duplicates this way, but the storage eliminates them
        for(int i = 0; i < network.getEdgeCount() + 1; i++) {
            Vector clique = new Vector();
            for(int k = 0; k < network.numChannels * 3 + 1; k++) {
                HashSet channel_cliques = (HashSet)clique_list.get(k);
                for(int j = 0; j < network.getEdgeCount() + 1; j++) {
                    if(network.interferes[i][j][k]) {
                        clique.addElement(j);
                    }
                }
                if(clique.size() > 0) {  
                    clique.addElement(i);        
                    if(clique.size() > network.getEdgeCount()) {
                        System.out.println("*********** ERROR ************");
                    }
                    channel_cliques.add(new HashSet(clique));
                    clique.removeAllElements(); 
                }
                clique_list.put(k, channel_cliques);
            }
        }

        //Print out all cliques to make sure we're good
        for(int k = 0; k < network.numChannels * 3 + 1; k++) {
            System.out.println("Cliques for channel " + k + ":");
            HashSet cliques = (HashSet)clique_list.get(k);
            for (Object c : cliques) {
                HashSet clique = (HashSet)c;
                System.out.print("\t[" + clique.size() + "] ");
                System.out.println(clique);
            }
        }


        // Build ILP
        try {
            IloCplex cplex = new IloCplex();
            // Objective function in Equation 3 - Minimize Overall Channel Costs
            IloLinearNumExpr cost = cplex.linearNumExpr();

            // Equation 2
            // Channel costs are all the same 1.0 for now
            double[] channel_costs = new double[network.numChannels * 3 + 1];
            Random randomGenerator = new Random();
            for(int k = 0; k < network.numChannels * 3 + 1; k++) {
                channel_costs[k] = randomGenerator.nextFloat();
                channel_costs[k] = 1.0;
            }

            // Equation 1 + Equation 3
            // Channel usage array (Number of edges * number of channels per edge)
            for(Object o: network.getEdges()) {
                Edge e = (Edge)o;
                for (int k = 0; k < e.channels.length; k++) {
                    if (e.channels[k] > 0.0) {
                        cost.addTerm(channel_costs[k], cplex.intVar(0,1, "c("+e.id+")("+k+")"));
                    }
                }
            }
            IloObjective objective = cplex.minimize(cost);
            cplex.add(objective);

            // Subject to Constraints
            // Equation 4
            // The largest clique (in theory) would include all edges, therefore the size
            int max_id = 0;
            for(Object o: network.getEdges()) {
                Edge e = (Edge)o;
                if (e.id > max_id) {
                    max_id = e.id;
                }
            }
            IloIntVar[][][] x = new IloIntVar[max_id+1][network.numChannels*3+1][network.getEdgeCount()+1];

            for(Object o: network.getEdges()) {
                Edge e = (Edge)o;
                for(int k = 0; k < e.channels.length; k++) {
                    // Find max clique involving (e_ik, c)
                    HashSet channel_cliques = (HashSet)clique_list.get(k);
                    int max_clique = findMaxClique(channel_cliques, e.id);
                    for(int c = 0; c < network.getEdgeCount(); c++) {
                        x[e.id][k][c] = cplex.intVar(0,1, "x("+e.id+")("+k+")("+c+")");
                        if (c == max_clique && e.channels[k] > 0.0) {
                            cplex.addEq(1, x[e.id][k][c]);
                        } else {
                            cplex.addEq(0, x[e.id][k][c]);
                        }
                    }
                }
            }

            // Equation 5
            // float[][][] D = float[network.getEdgeCount()][network.numChannels][network.getEdgeCount()];

            // Equation 6

            // Equation 7

            // Equation 8

            // Equation 9
            
            // Write the model out to validate
            cplex.exportModel("JRCS-TVWS.lp");

            if ( cplex.solve() ) { 
                System.out.println("Solution status = " + cplex.getStatus()); 
                System.out.println("Solution value  = " + cplex.getObjValue());
              }
            cplex.end();
        } catch (IloException e) {
            System.err.println("Concert exception '" + e + "' caught.");
        }
        // Show graph
        if (options.display) {
            drawing = new Draw(network, 1024, 768,
                    "Routing and Channel Selection Application");
            drawing.draw();
        }

        System.out.println("Seed, Width, Height, Nodes, Users, Channels");
        System.out.println(options.seed + ", " + options.width + ", " + options.height + ", " + options.relays + ", " +
                options.subscribers + ", " + options.channels);

        // System.out.println("RCS Bisection Bandwidth:");
        // for(int i = 0; i < rcsBisectionBandwidth.length; i++) {
        //     System.out.print(i + " :");
        //     for(int j = 0; j < rcsBisectionBandwidth[i].length;j++) {
        //         System.out.printf(" %7.2g", rcsBisectionBandwidth[i][j]);
        //     }
        //     System.out.print("\n");
        // }
    }
}
