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

    public static Boolean grows_clique(HashSet clique, Edge edge, Network network, int channel) {

        if (clique.size() > network.getEdgeCount()) {
            return(false);
        }
 
        if (edge.channels[channel] == 0.0d) {
            return(false);
        }

        for(Object o: clique) {
            Edge e = (Edge)o;

            if (e.id == edge.id) {
                return(false);
            }

            if ((! network.interferes[e.id][edge.id][channel]) && (!network.interferes[edge.id][e.id][channel])) {
                return(false);
            }
        }
        return(true);
    }

    public static HashMap enumerate_cliques(Network network) {
        HashMap clique_list = new HashMap();
        HashSet clique = new HashSet();

        // For each edge
        for (Object o : network.getEdges()) {
            Edge e = (Edge)o;
            HashMap edge_cliques = new HashMap();
            // For each channel
            for(int k = 0; k < network.numChannels * 3; k++) {
                HashMap size_cliques = new HashMap();
                HashSet cliques = new HashSet();
                clique.clear();

                // Clique of size 1, if it's using channel k
                if(e.channels[k] > 0.0) {
                    clique.add(e);  
                }

                // Every edge is a clique of size 1, and there's only one clique of size 1
                if (clique.size() > 0) {
                    cliques.add(clique.clone());
                }
                size_cliques.put(1,cliques.clone());

                // Check all sizes up to max
                for(int i = 2; i < 5; i++) {
                    // Clear all cliques from the size list
                    cliques.clear();

                    // For each clique of the size i-1
                    for(Object p: (HashSet)size_cliques.get(i-1)) {
                        clique = (HashSet)p;

                        // Check to see if adding an edge will grow them
                        for(Object q: network.getEdges()) {
                            Edge edge = (Edge)q;

                            // If it grows the clique
                            if (grows_clique(clique, edge, network, k)) {

                                // Add the edge
                                clique.add(edge);

                                // Store a copy under this size list
                                cliques.add(clique.clone());

                                // Remove the edge
                                clique.remove(edge);
                            }
                        }
                    }
                    size_cliques.put(i, cliques.clone());
                }

                // store cliques
                edge_cliques.put(k, size_cliques.clone());
            }
            // roll out of the loop, storing results
            clique_list.put(e.id, edge_cliques.clone());
        }
        return(clique_list);        
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

        // Renumber edges
        int eid = 0;
        for (Object o: network.getEdges()) {
            Edge e = (Edge)o;
            e.setId(eid);
            eid += 1;
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
        // indexed by edge, then channel, then a list of cliques
        HashMap clique_list = enumerate_cliques(network);

        //Print out all cliques to make sure we're good
        for(Object o: network.getEdges()) {
            Edge e = (Edge)o;
            HashMap edge_cliques = (HashMap)clique_list.get(e.id);
            for(int k = 0; k < network.numChannels * 3; k++) {
                System.out.println("Cliques involving Edge: " + e.id + " using channel " + k + ":");
                // Keys are size, values are a list of cliques of that size
                HashMap cliques_of_size_key = (HashMap)edge_cliques.get(k);
                for(Object t: cliques_of_size_key.keySet()) {
                    Integer size = (Integer)t;
                    HashSet n_cliques = (HashSet)cliques_of_size_key.get(size);
                    System.out.print("\t[ Size: " + size + " #: "+ n_cliques.size() + "] ");
                    System.out.println(n_cliques);
                }
            }
        }


        // Build ILP
        try {
            IloCplex cplex = new IloCplex();

            // Variable Definitions for ILP
            // c
            IloIntVar [] c = new IloIntVar[network.numChannels * 3]; 
            for(int i = 0; i < network.numChannels * 3; i++) {
                c[i] = cplex.intVar(0,1, "c("+i+")");
            }

            // x
            IloIntVar[][][] x = new IloIntVar[network.getEdgeCount()][network.numChannels*3+1][network.getEdgeCount()];
            for(Object o: network.getEdges()) {
                Edge e = (Edge)o;
                for(int k = 0; k <  network.numChannels*3; k++) {
                    for(int tc = 0; tc < network.getEdgeCount(); tc++) {
                        x[e.id][k][tc] = cplex.intVar(0,1, "x("+e.id+")("+k+")("+tc+")");
                    }
                }
            }

            // D
            IloNumVar[][][] D = new IloNumVar[network.getEdgeCount()][network.numChannels*3][network.getEdgeCount()];
            for(Object o: network.getEdges()) {
                Edge e = (Edge)o;
                for(int k = 0; k < network.numChannels*3; k++) {
                    for(int tc = 0; tc < network.getEdgeCount(); tc++) {
                        D[e.id][k][tc] = cplex.numVar(0,1, "D("+e.id+")("+k+")("+tc+")");
                    }
                }
            }

            // y
            IloIntVar[][] y = new IloIntVar[network.getEdgeCount()][network.numChannels*3];
            for(Object o: network.getEdges()) {
                Edge e = (Edge)o;
                for(int k = 0; k <  network.numChannels*3; k++) {
                    y[e.id][k] = cplex.intVar(0,1, "y("+e.id+")("+k+")");
                }
            }

            // Objective function in Equation 3 - Minimize Overall Channel Costs
            IloLinearNumExpr cost = cplex.linearNumExpr();

            // Equation 2
            // Channel costs are all the same 1.0 for now
            double[] channel_costs = new double[network.numChannels * 3];
            Random randomGenerator = new Random();
            for(int k = 0; k < network.numChannels * 3; k++) {
                channel_costs[k] = randomGenerator.nextFloat();
                channel_costs[k] = 1.0;
            }

            // Equation 3: Minimize channel costs (This is the objective)
            // Channel usage array (Number of edges * number of channels per edge)
            for(Object o: network.getEdges()) {
                Edge e = (Edge)o;
                for (int k = 0; k < e.channels.length; k++) {
                    if (e.channels[k] > 0.0) {
                        cost.addTerm(channel_costs[k], c[k]);
                    }
                }
            }
            IloObjective objective = cplex.minimize(cost);
            cplex.add(objective);

            // Subject to Constraints
            // Equation 4: Variable x contains max, clique, channel activation status
            // The largest clique (in theory) would include all edges, therefore the size
            // int max_id = 0;
            // for(Object o: network.getEdges()) {
            //     Edge e = (Edge)o;
            //     if (e.id > max_id) {
            //         max_id = e.id;
            //     }
            // }
            // int[][] max_cliques = new int[max_id+1][network.numChannels*3+1];
            // for(Object o: network.getEdges()) {
            //     Edge e = (Edge)o;
            //     for(int k = 0; k < e.channels.length; k++) {
            //         // Find max clique involving (e_ik, c)
            //         HashSet channel_cliques = (HashSet)clique_list.get(k);
            //         int max_clique = findMaxClique(channel_cliques, e.id);
            //         for(int mc = 1; mc < network.getEdgeCount(); mc++) {
            //             x[e.id][k][mc] = cplex.intVar(0,1, "x("+e.id+")("+k+")("+mc+")");
            //             if (mc == max_clique && e.channels[k] > 0.0) {
            //                 cplex.addEq(1, x[e.id][k][mc]);
            //                 max_cliques[e.id][k] = mc;
            //             } else {
            //                 cplex.addEq(0, x[e.id][k][mc]);
            //             }
            //         }
            //     }
            // }

            // Equation 5: break up capacity by clique size
            // for(Object o: network.getEdges()) {
            //     Edge e = (Edge)o;
            //     for(int k = 0; k < network.numChannels*3+1; k++) {
            //         for(int mc = 1; mc < network.getEdgeCount(); mc++) {
            //             D[e.id][k][mc] = e.channels[k] / mc;
            //             cplex.addEq(e.channels[k]/tc, D[e.id][k][tc]);
            //         }
            //     }
            // }

            // Equation 6:

            // Equation 7: 

            // Equation 8: 

            // Equation 9: Total capacity constraint
            
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
