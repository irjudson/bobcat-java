package bobcat.applications;

import bobcat.network.*;
import bobcat.simulation.Draw;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.algorithms.shortestpath.PrimMinimumSpanningTree;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;
import ilog.concert.*;
import ilog.cplex.IloCplex;
import org.apache.commons.collections15.Transformer;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.util.*;

public class WhitespaceShortCircuit {

	static int MAX_CLIQUE_SIZE = 4;

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

	public static Boolean grows_clique(HashSet clique, Edge edge, Network network, int channel) {

		if (clique.size() > network.getEdgeCount()) {
			// System.out.println("Clique already max size.");
			return (false);
		}

		if (edge.channels[channel] <= 1.0) {
			// System.out.println("Edge has no throughput.");
			return (false);
		}

		for (Object o : clique) {
			Edge e = (Edge) o;

			if (e.id == edge.id) {
				// System.out.println("Clique already contains edge.");
				return (false);
			}

			if ((!network.interferes[e.id][edge.id][channel]) && (!network.interferes[edge.id][e.id][channel])) {
				// System.out.println("No interference for the edge/clique on this channel.");
				return (false);
			}
		}
		return (true);
	}

	public static HashMap enumerate_cliques(Network network) {
		HashMap clique_list = new HashMap();

		// For each edge
		for (Object o : network.getEdges()) {
			Edge e = (Edge) o;
			HashMap channel_cliques = new HashMap();

			// For each channel
			for (int k = 0; k < network.numChannels * 3; k++) {
				HashMap size_cliques = new HashMap();
				HashSet cliques = new HashSet();
				HashSet clique = new HashSet();
				clique.clear();

				// Clique of size 1, if it's using channel k
				if (Math.abs(e.channels[k]) > 1.0) {

					clique.add(e);
					cliques.add(new HashSet(clique));
					// System.out.println("Adding clique of size 1 for channel: "+k+" ("+e.channels[k]+") "+clique);
					size_cliques.put(1, new HashSet(cliques));
					cliques.clear();

					// Check all sizes up to max
					for (int i = 2; i < MAX_CLIQUE_SIZE; i++) {

						// For each clique of the size i-1
						for (Object p : (HashSet) size_cliques.get(i - 1)) {
							clique = (HashSet) p;

							// Check to see if adding an edge will grow them
							for (Object q : network.getEdges()) {
								Edge edge = (Edge) q;

								// If it grows the clique
								if (grows_clique(clique, edge, network, k)) {

									HashSet cliqueCopy = new HashSet(clique);
									// clique.clear();
									cliqueCopy.add(edge);

									if (cliqueCopy.size() == i) {
										// Store a copy under this size list
										cliques.add(cliqueCopy);
										// System.out.println("New clique of size: " + i + " " + cliqueCopy);
									}
								} else {
									// System.out.println("Adding "+edge+" doesn't grow clique "+clique);
								}
							}
						}
						size_cliques.put(i, new HashSet(cliques));
						cliques.clear();
					}
					// store cliques
					channel_cliques.put(k, new HashMap(size_cliques));
					size_cliques.clear();
				} else {
					// System.out.println("No clique for edge with no throughput.");
					// System.out.println("Edge "+e.id+ " Channel "+k + ": "+e.channels[k]);
				}
			}
			// roll out of the loop, storing results
			clique_list.put(e.id, new HashMap(channel_cliques));
			channel_cliques.clear();
		}

		return (clique_list);
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
			networkGenerator = Network.getGenerator(options.relays, options.subscribers, options.width, options.height, options.seed + count, options.channels, options.channelProb);
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
			System.out.println("Prim Tree: ");
			System.out.print(primTree.toString());
			System.out.println("");
		}

		for (Object e : primTree.getEdges()) {
			// Color the MST
			((Edge) e).type = 2;
		}

		Collection<Edge> primEdges = primTree.getEdges();
		Vector toRemove = new Vector();
		for (Object e : network.getEdges()) {
			if (!primEdges.contains(e)) {
				toRemove.addElement(e);
			}
		}

		for (Object e : toRemove) {
			network.removeEdge((Edge) e);
		}

		// Renumber nodes
		int nid = 0;
		for(Object o : network.getVertices()) {
			Vertex v = (Vertex)o;
			v.id = nid;
			nid += 1;
		}
		// Renumber edges
		int eid = 0;
		for (Object o : network.getEdges()) {
			Edge e = (Edge) o;
			e.setId(eid);
			eid += 1;
		}

		network.computeInterference();

		DijkstraShortestPath<Vertex, Edge> dspath = new DijkstraShortestPath(primTree);

		Double[] demand = new Double[network.getEdgeCount()];

		for(Object o : network.getEdges()) {
			Edge e = (Edge)o;
			demand[e.id] = 0.0d;
		}
		// Select a random set of (s,t) and set the connection requests along that path to 2e7
		Random rg = new Random();
		for (int i = 0; i < 5; i++) {
			Vertex s = network.getVertex(rg.nextInt(network.getVertexCount()+1));
			Vertex t = network.getVertex(rg.nextInt(network.getVertexCount()+1));
			if (s != t && s != null && t != null) {
				List<Edge> spath = dspath.getPath(s,t);
				System.out.println("Path #"+i+": ("+s+","+t+") :"+spath);
				for(Object o: spath) {
					Edge e = (Edge)o;
					demand[e.id] += 2e7;
				}
			} else {
				i--;
			}
		}

		// go over all the nxn nodes and find the throughput using rcs
		for (Object o : network.getVertices()) {
			Vertex source = (Vertex) o;
			for (Object p : network.getVertices()) {
				Vertex destination = (Vertex) p;
				if (source.id < destination.id) {
					List<Edge> spath = dspath.getPath(source, destination);
					Edge c = null;
					for (Object q : spath) {
						Edge e = (Edge)q;
						if (c == null || e.capacity < c.bottleNeckCapacity()) {
							c = e;
						}
					}
					if (options.verbose) {
						System.out.println(source + " -> " + destination + " : " + spath);						
					}
				// System.out.println(source + " -> " + destination + " : "+c.id+" : bnc:  "+c.bottleNeckCapacity() + " demand: " + demand[c.id]);
				}
			}
		}

		// Print out the edge list // if (options.verbose) {
			System.out.println(network.getEdgeCount() + " Edges");
			for (Object e : network.getEdges()) {
				Edge z = (Edge)e;
				System.out.println("\t" + z + " Bottleneck Capacity: " + z.bottleNeckCapacity() + " Demand: "+demand[z.id]);
			}
		// }

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

		if (options.verbose) {
			//Print out all cliques to make sure we're good
			for (Object o : network.getEdges()) {
				Edge e = (Edge) o;
				HashMap edge_cliques = (HashMap) clique_list.get(e.id);
				for (int k = 0; k < network.numChannels * 3; k++) {
					System.out.println("Cliques involving Edge: " + e.id + " using channel " + k + ":");
					// Keys are size, values are a list of cliques of that size
					HashMap cliques_of_size_key = (HashMap) edge_cliques.get(k);
					if (cliques_of_size_key != null) {
						for (Object t : cliques_of_size_key.keySet()) {
							Integer size = (Integer) t;
							HashSet clique = (HashSet) cliques_of_size_key.get(size);
							System.out.print("\t[ Size: " + size + " #: " + clique.size() + "] ");
							System.out.println(clique);							
						}
					}
				}
			}
		}


		// Build ILP
		try {
			IloCplex cplex = new IloCplex();

			// Variable Definitions for ILP
			// c
			IloIntVar[] c = new IloIntVar[network.numChannels * 3];
			for (int i = 0; i < network.numChannels * 3; i++) {
				c[i] = cplex.intVar(0, 1, "c(" + i + ")");
			}

			// x
			IloIntVar[][][] x = new IloIntVar[network.getEdgeCount()][network.numChannels * 3][MAX_CLIQUE_SIZE];
			for (Object o : network.getEdges()) {
				Edge e = (Edge) o;
				for (int k = 0; k < network.numChannels * 3; k++) {
					for (int tc = 1; tc < MAX_CLIQUE_SIZE; tc++) {
						// System.out.println(x.length+","+x[0].length+","+x[0][0].length+ "  "+e.id+","+k+","+tc);
						x[e.id][k][tc] = cplex.intVar(0, 1, "x(" + e.id + ")(" + k + ")(" + tc + ")");
					}
				}
			}

			// D
			Double[][][] D = new Double[network.getEdgeCount()][network.numChannels * 3][MAX_CLIQUE_SIZE];
			for (Object o : network.getEdges()) {
				Edge e = (Edge) o;
				for (int k = 0; k < network.numChannels * 3; k++) {
					for (int size = 1; size < MAX_CLIQUE_SIZE; size++) {
						if (e.channels[k] >= 0) {
							D[e.id][k][size] = e.channels[k] / size;	
						} else {
							D[e.id][k][size] = 0.0d;
						}
					}
				}
			}

			// y
			IloIntVar[][] y = new IloIntVar[network.getEdgeCount()][network.numChannels * 3];
			for (Object o : network.getEdges()) {
				Edge e = (Edge) o;
				for (int k = 0; k < network.numChannels * 3; k++) {
					y[e.id][k] = cplex.intVar(0, 1, "y(" + e.id + ")(" + k + ")");
				}
			}

			// Objective function in Equation 3 - Minimize Overall Channel Costs
			IloLinearNumExpr cost = cplex.linearNumExpr();

			// Initialize Channel Costs: Channel costs are all the same 1.0 for now
			double[] channel_costs = new double[network.numChannels * 3];
			Random randomGenerator = new Random();
			for (int k = 0; k < network.numChannels * 3; k++) {
                if (k % 3 == 0) {
                	channel_costs[k] = 1.0;
                }
                if (k % 3 == 1) {
                	channel_costs[k] = 1.0;
                }
                if (k % 3 == 2) {
                	channel_costs[k] = 1.0;
                }
			}

			// Objective: Minimize channel costs 
			// Channel usage array (Number of edges * number of channels per edge)
			for (int k = 0; k < network.numChannels * 3; k++) {
				cost.addTerm(channel_costs[k], c[k]);
			}
			IloObjective objective = cplex.minimize(cost);
			cplex.add(objective);
			System.out.println("Objective : "+cost);

			// Constraint 1:
			System.out.println("Constraint 1:");
			for (Object o : network.getEdges()) {
				Edge e = (Edge) o;
				for (int k = 0; k < network.numChannels * 3; k++) {
					// Sum acros clique sizes
					IloNumExpr irj = cplex.numExpr();
					irj = x[e.id][k][1];
					for (int tc = 2; tc < MAX_CLIQUE_SIZE; tc++) {
						irj = cplex.sum(x[e.id][k][tc], irj);
					}
					System.out.println("\t"+y[e.id][k] + " = " + irj);
					cplex.addEq(y[e.id][k], irj);
					System.out.println("\t"+y[e.id][k] + " <= " + c[k]);
					cplex.addLe(y[e.id][k], c[k]);
				}
			}

			// Constraint 2: 
			System.out.println("Constraint 2:");
			for (Object o : network.getEdges()) {
				Edge e = (Edge) o;
				for (int k = 0; k < network.numChannels * 3; k++) {
					// Sum acros clique sizes
					IloNumExpr irj = cplex.numExpr();
					irj = x[e.id][k][1];
					for (int tc = 2; tc < MAX_CLIQUE_SIZE; tc++) {
						irj = cplex.sum(x[e.id][k][tc], irj);
					}
					System.out.println("\t"+irj+" <= " + 1);
					cplex.addLe(irj, 1);
				}
			}

			// Constraint 3:
			System.out.println("Constraint 3:");
			// Make interim data structure to make this easier
			HashMap cl = new HashMap();
			for (Object o : network.getEdges()) {
				Edge e = (Edge) o;
				HashMap edge_cliques = (HashMap) clique_list.get(e.id);
				for (int k = 0; k < network.numChannels * 3; k++) {
					HashMap cliques_of_size = (HashMap)edge_cliques.get(k);
					if (cliques_of_size != null) {
						for (int c1 = 1; c1 < MAX_CLIQUE_SIZE; c1++) {
							HashSet cliques = (HashSet)cliques_of_size.get(c1);
							HashSet cls = (HashSet)cl.get(k);
							if (cls == null) {
								cl.put(k, new HashSet(cliques));
							} else {
								cls.addAll(cliques);
								cl.put(k, cls);
							}
						}
					}
				}
			}

			for (int k = 0; k < network.numChannels * 3; k++) {
				HashSet cliques = (HashSet)cl.get(k);
				if (cliques != null) {
					for (Object o : cliques) {
						HashSet clique = (HashSet)o;
						IloNumExpr cs2 = cplex.numExpr();
						Object[] cl2 = clique.toArray();
						Edge xx = (Edge)cl2[0];
						cs2 = y[xx.id][k];
						for (int i = 1; i < clique.size(); i++) {
							Edge xxx = (Edge)cl2[i];
							cs2 = cplex.sum(y[xxx.id][k], cs2);
						}
						IloNumExpr c8 = cplex.diff(clique.size(), cs2);
						for (Object p : clique) {
							Edge e = (Edge)p;
							for (int i = 1; i < clique.size() - 1; i++) {
								System.out.println("\t"+x[e.id][k][i] + " <= " + c8);
								cplex.addLe(x[e.id][k][i], c8);
							}
						}
					}
				}
			}


			// Constraint 4: Total capacity constraint
			System.out.println("Constraint 4:");
			for (Object zz : network.getEdges()) {
				Edge e = (Edge) zz;
				IloNumExpr lhs = cplex.numExpr();
				for (int k = 0; k < network.numChannels * 3; k++) {
					for (int tc = 1; tc < MAX_CLIQUE_SIZE; tc++) {
						lhs = cplex.sum(cplex.prod(D[e.id][k][tc], x[e.id][k][tc]), lhs);
					}
				}
				// Calculate demand
				System.out.println("\t"+lhs + " >= "+ demand[e.id]);
				cplex.addGe(lhs, demand[e.id]);
			}

			// Write the model out to validate
			cplex.exportModel("CRTC.lp");

			if (cplex.solve()) {
				double cplexTotal = cplex.getObjValue();

				for (Object o : network.getEdges()) {
					Edge e = (Edge) o;
					double sum = 0.0;
					for (int k = 0; k < network.numChannels * 3; k++) {
						for (int tc = 1; tc < MAX_CLIQUE_SIZE; tc++) {
							double value = cplex.getValue(x[e.id][k][tc]);
							if (value > 0) {
								sum += D[e.id][k][tc];
								System.out.println("\tEdge: "+e.id+" Channel: "+k+" Throughput: "+D[e.id][k][tc]);
							}
							System.out.println("x("+e.id+")("+k+")("+tc+") = " + cplex.getValue(x[e.id][k][tc]));
						}
					}
					System.out.println("Edge: "+e.id+" Total Contribution: "+sum);
				}

				for (Object o : network.getEdges()) {
					Edge e = (Edge) o;
					for (int k = 0; k < network.numChannels * 3; k++) {
						System.out.println("y("+e.id+")("+k+") = " + cplex.getValue(y[e.id][k]));
					}
				}

				for (int i = 0; i < network.numChannels * 3; i++) {
					System.out.println("c("+i+") = "+cplex.getValue(c[i]));
				}

				System.out.println("Solution status = " + cplex.getStatus());
				System.out.println("Solution value  = " + cplexTotal);
			} else {
				System.out.println("Couldn't solve problem!");
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
