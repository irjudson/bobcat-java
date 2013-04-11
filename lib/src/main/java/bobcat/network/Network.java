package bobcat.network;

import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Hypergraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.io.GraphIOException;
import edu.uci.ics.jung.io.GraphMLWriter;
import edu.uci.ics.jung.io.graphml.GraphMLReader2;
import edu.uci.ics.jung.io.graphml.GraphMetadata;
import edu.uci.ics.jung.io.graphml.NodeMetadata;
import edu.uci.ics.jung.io.graphml.EdgeMetadata;
import edu.uci.ics.jung.io.graphml.HyperEdgeMetadata;

import java.util.*;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.PrintWriter;
import java.awt.geom.Point2D;

import org.apache.commons.collections15.Transformer;

public class Network<V, E>
    extends UndirectedSparseGraph<V, E>
    implements Graph<V, E> {

    final public double width;
    final public double height;
    public Vertex gateway;
    public HashSet relays;
    public HashSet subscribers;
    public Vertex[] relayList;
    public Vertex[] subList;
    public static double meanQueueLength;
    public static double timeslotLength;
    public static int[] thetaSet = new int[1]; // Brendan added, for now just two theta kept
    public HashSet<Vertex>[][][] beamSet; // beamSet[i][k][l] = lth beam set for relay i, theta k
    public static int numChannels;
    public static double channelProb;
    public Random random;
    public long seed;
    public boolean[][][] interferes;

    public Network(double size, int numChannels, int numEdges) {
	this.width = size;
	this.height = size;
	this.numChannels = numChannels;
	this.interferes = new boolean[numEdges + 1][numEdges + 1][numChannels * 3 + 1];
    }

    public Network(double width, double height, int theta, int channels) {
        this.width = width;
        this.height = height;
        this.numChannels = channels;
        this.thetaSet[0] = theta;
    }

    /**
     * Returns a {@code Generator} that creates an instance of this graph type.
     * @param <V> the vertex type for the graph factory
     * @param <E> the edge type for the graph factory
     */
    public static NetworkGenerator getGenerator(int numRelays, 
						int numSubscribers, 
						int sectors, double width, 
						double height, Random random,
						long seed,
						int theta, double meanq, 
						double slotlen, int channels, 
						double prob, boolean quick) {

        NetworkGenerator gen = new NetworkGenerator(new NetworkFactory(width, height, theta, channels),
                                                    new VertexFactory(width, height, sectors, meanq, random),
                                                    new EdgeFactory(), numRelays, numSubscribers, width, height, 
						    random, quick);
        thetaSet[0] = theta;
        meanQueueLength = meanq;
        timeslotLength = slotlen;
        numChannels = channels;
        channelProb = prob;
	seed = seed;
        return (gen);
    }

    public static NetworkGenerator getGenerator(int numRelays, int numSubscribers, 
						int sectors, double width, 
						double height, long seed, int theta, 
						double meanq, double slotlen, 
						int channels, double prob) {

        NetworkGenerator gen = new NetworkGenerator(new NetworkFactory(width, height, theta, channels),
                                                    new VertexFactory(width, height, sectors, meanq, seed),
                                                    new EdgeFactory(), numRelays, numSubscribers, width, height, seed);
        thetaSet[0] = theta;
        meanQueueLength = meanq;
        timeslotLength = slotlen;
        numChannels = channels;
        channelProb = prob;
	seed = seed;
        return (gen);
    }

    public static NetworkGenerator getGenerator(int numRelays, int numSubscribers, double width, double height, 
                                                long seed, int channels, double prob) {

        NetworkGenerator gen = new NetworkGenerator(new NetworkFactory(width, height, channels),
                                                    new VertexFactory(width, height, seed),
                                                    new EdgeFactory(), numRelays, numSubscribers, width, height, seed);
        numChannels = channels;
        channelProb = prob;
	seed = seed;
        return (gen);
    }

    public Edge getEdge(int id) {
        for (Object o: this.getEdges()) {
            Edge e = (Edge)o;
            if (e.id == id) {
                return(e);
            }
        }
        return(null);
    }

    public Vertex getVertex(int id) {
        for (Object o: this.getVertices()) {
            Vertex v = (Vertex)o;
            if (v.id == id) {
                return(v);
            }
        }
        return(null);
    }

    public static NetworkGenerator getGenerator(int relays, int subscribers, 
                                                double width, double height, 
                                                Random random, long seed, 
						int channels, double prob, 
						boolean quick) {
        NetworkGenerator gen = new NetworkGenerator(new NetworkFactory(width, 
                                                                       height, 
                                                                       channels),
                                                    new VertexFactory(width, height, random),
                                                    new EdgeFactory(), relays, subscribers, width, height, random, quick);
        numChannels = channels;
        channelProb = prob;
	seed = seed;
        return (gen);
    }

    public Vertex randomRelay() {
        Vector<V> v = new Vector<V>(this.getVertices());
        return((Vertex)v.get(this.random.nextInt(v.size())));
    }

    public void printInterferenceTable() {
        for (int i = 0; i < this.interferes.length; i++) {
            for (int j = 0; j < this.interferes[i].length; j++) {
                for (int k = 0; k < this.interferes[i][j].length; k++) {
                    System.out.println(i+","+j+","+k+"="+this.interferes[i][j][k]);
                }
            }
        }
    }

    public void computeInterference() {
        //Build interference table
        for (E e1a : this.getEdges()) {
            Edge e1 = (Edge)e1a;
            for (E e2a : this.getEdges()) {
                Edge e2 = (Edge)e2a;
                if (e1 != e2) {
                    Pair<V> v1a = this.getEndpoints(e1a);
                    Pair<V> v2a = this.getEndpoints(e2a);
                    Vertex v1f = (Vertex) v1a.getFirst();
                    Vertex v1s = (Vertex) v1a.getSecond();
                    Vertex v2f = (Vertex) v2a.getFirst();
                    Vertex v2s = (Vertex) v2a.getSecond();
                    // System.out.println("Edges: (" + e1.id + " : " + v1f.id + "," + v1s.id+") (" + e2.id + " : " + v2f.id + "," + v2s.id+")");
                    double dist = v1f.distanceTo(v2f);
                    double d2 = v1f.distanceTo(v2s);
                    double d3 = v1s.distanceTo(v2f);
                    double d4 = v1s.distanceTo(v2s);
                    dist = (dist < d2) ? dist : d2;
                    dist = (dist < d3) ? dist : d3;
                    dist = (dist < d4) ? dist : d4;
                    for (int i = 0; i < this.numChannels * 3; i++) {
                        double range = 0.0d;
                        if (i > 0 && i < this.numChannels) {
                            range = 30.8;
                        } else if (i >= 0 + this.numChannels &&
				   i < 2 * this.numChannels) {
                            range = 9.0;
                        } else if (i >= 0 + 2 * this.numChannels &&
				   i < 3 * this.numChannels) {
                            range = 3.6;
                        }
                        // System.out.println(" - "+i+" -> D: " + dist + " R: "+ range);
                        // System.out.println(v1f+" "+v1s+" "+v2f+" "+v2s);
                        if (e1.channels[i] > 0.0 && e2.channels[i] > 0.0) {
                            if (dist < range || (v1f == v2f || v1f == v2s || v1s == v2s || v1s == v2f)) {
                                this.interferes[e1.id][e2.id][i] = true;
                                this.interferes[e2.id][e1.id][i] = true;                                
                            } else {
                                this.interferes[e1.id][e2.id][i] = false;
                                this.interferes[e2.id][e1.id][i] = false;                                                            
                            }
                        }
                    }
                }
            }
        }        
    }

    public void calculateBeamSets() {
        int numRelays = relayList.length;
        int numSubs = subList.length;
        this.beamSet = new HashSet[numRelays][this.thetaSet.length][];

        for (int i = 0; i < relayList.length; i++) {
            Vertex relay = relayList[i];

            for (int k = 0; k < this.thetaSet.length; k++) {
                BearingSub[] sortedSubs = new BearingSub[numSubs];
                int ns = 0;
                for (int j = 0; j < subList.length; j++) {
                    Vertex sub = subList[j];
                    double bearing = relay.getBearing(sub);
                    if (relay.calculateThroughput(this.thetaSet[k], sub) > 0) {
                        sortedSubs[ns++] = new BearingSub(bearing, sub);
                    }
                }
                sortedSubs = Arrays.copyOf(sortedSubs, ns);
                Arrays.sort(sortedSubs);

                this.beamSet[i][k] = new HashSet[0];
                ArrayList<HashSet<Vertex>> tmp = new ArrayList();
                for (int start = 0; start < sortedSubs.length; start++) {
                    HashSet<Vertex> nextSet = new HashSet<Vertex>();
                    int end;
                    end = start;
                    double endBearing = sortedSubs[end].bearing;
                    while (endBearing - sortedSubs[start].bearing <= this.thetaSet[k]) {
                        nextSet.add(sortedSubs[end].sub);
                        end = (end + 1) % sortedSubs.length;
                        if (end == start)
                            break;
                        endBearing = sortedSubs[end].bearing;
                        if (endBearing < sortedSubs[start].bearing) {
                            endBearing += 360.0;
                        }
                    }
                    if (tmp.isEmpty() || !tmp.get(tmp.size() - 1).containsAll(nextSet)) {
                        tmp.add(nextSet);
                    }
                }
                this.beamSet[i][k] = tmp.toArray(this.beamSet[i][k]);
            }
        }
    }

    public static Network LoadNetwork(String filename) {
	Network n = null;
	try {
	    FileReader in = new FileReader(filename);
	    Transformer<GraphMetadata, Graph<Vertex, Edge>> gTrans = new Transformer<GraphMetadata, Graph<Vertex, Edge>>() { 
		public Graph<Vertex, Edge> transform(GraphMetadata metadata) {
		    double size = Double.parseDouble(metadata.getProperty("size"));
		    int channels = Integer.parseInt(metadata.getProperty("channels"));
		    int edgeCount = Integer.parseInt(metadata.getProperty("numEdges"));
		    return(new Network(size, channels, edgeCount));
		}
	    };
 	    Transformer<NodeMetadata, Vertex> vTrans = new Transformer<NodeMetadata, Vertex>(){ 
 		public Vertex transform(NodeMetadata metadata) {
		    int i = Integer.parseInt(metadata.getId());
		    double x = Double.parseDouble(metadata.getProperty("x"));
		    double y = Double.parseDouble(metadata.getProperty("y"));
		    Vertex v = new Vertex(i,x,y);
		    v.type = Integer.parseInt(metadata.getProperty("type")); 
		    return(v);
 		}
 	    };
 	    Transformer<EdgeMetadata, Edge> eTrans = new Transformer<EdgeMetadata, Edge>(){ 
 		public Edge transform(EdgeMetadata metadata) {
		    int numChannels = Integer.parseInt(metadata.getProperty("numChannels"));
		    int i = Integer.parseInt(metadata.getId());
		    Edge e = new Edge(i);
		    e.channels = new Double[numChannels];
		    for(int ij = 0; ij < numChannels; ij++) {
			e.channels[ij] = 0.0d;
		    }
		    String enc = metadata.getProperty("channels");
		    String[] chs = enc.split(" ");
		    for(Object cha: chs) {
			String as = (String)cha;
			String[] a = as.split(":");
			int idx = Integer.parseInt(a[0]);
			double thpt = Double.parseDouble(a[1]);
			e.channels[idx] = thpt;
		    }
		    e.capacity = Double.parseDouble(metadata.getProperty("capacity"));
		    e.length = Double.parseDouble(metadata.getProperty("length"));
		    e.weight = Double.parseDouble(metadata.getProperty("weight"));
		    return(e);
 		}
 	    };
 	    Transformer<HyperEdgeMetadata,Edge> hTrans = new Transformer<HyperEdgeMetadata,Edge>(){ 
 		public Edge transform(HyperEdgeMetadata metadata) {
		    int i = Integer.parseInt(metadata.getId());
		    return(new Edge(i));
 		}
 	    };

	    GraphMLReader2<Graph<Vertex, Edge>, Vertex, Edge> gmlr =
		new GraphMLReader2<Graph<Vertex, Edge>, Vertex, Edge>(in, 
								      gTrans, vTrans, 
								      eTrans, hTrans);

	    try {
		n = (Network)gmlr.readGraph();
	    } catch (GraphIOException e) {}
	} catch (FileNotFoundException e) {}
	n.configure();
	return(n);
    }

    public void configure() {
    }

    public void SaveNetwork(String filename) {
	try {
	    GraphMLWriter<V,E> gmlw = new GraphMLWriter<V,E>();
	    PrintWriter out = new PrintWriter(new FileWriter(filename));
	    gmlw.setEdgeIDs(new Transformer<E, String>() {
				public String transform(E e1) {
				    Edge e = (Edge)e1;
				    return(String.valueOf(e.id));
				}
			    });
	    gmlw.addVertexData("type", null, "2",
			       new Transformer<V, String>() {
				   public String transform(V v1) {
				       Vertex v = (Vertex)v1;
				       return Integer.toString(v.type);
				   }
			       });
	    gmlw.addVertexData("x", null, "0.0",
			       new Transformer<V, String>() {
				   public String transform(V v1) {
				       Vertex v = (Vertex)v1;
				       return Double.toString(v.location.getX());
				   }
			       });
	    gmlw.addVertexData("y", null, "0.0",
			       new Transformer<V, String>() {
				   public String transform(V v1) {
				       Vertex v = (Vertex)v1;
				       return Double.toString(v.location.getY());
				   }
			       });
	    gmlw.addEdgeData("channels", null, "",
			     new Transformer<E, String>() {
				 public String transform(E e1) {
				     Edge e = (Edge)e1;
				     return(e.channelList());
				 }
			     });
	    gmlw.addEdgeData("numChannels", null, "",
			     new Transformer<E, String>() {
				 public String transform(E e1) {
				     Edge e = (Edge)e1;
				     return(String.valueOf(e.channels.length));
				 }
			     });
	    gmlw.addEdgeData("length", null, "",
			     new Transformer<E, String>() {
				 public String transform(E e1) {
				     Edge e = (Edge)e1;
				     return(String.valueOf(e.length));
				 }
			     });
	    gmlw.addEdgeData("capacity", null, "",
			     new Transformer<E, String>() {
				 public String transform(E e1) {
				     Edge e = (Edge)e1;
				     return(String.valueOf(e.capacity));
				 }
			     });
	    gmlw.addEdgeData("weight", null, "",
			     new Transformer<E, String>() {
				 public String transform(E e1) {
				     Edge e = (Edge)e1;
				     return(String.valueOf(e.weight));
				 }
			     });
 	    gmlw.addGraphData("size", null, "",
 			      new Transformer<Hypergraph<V,E>, String>() {
				  public String transform(Hypergraph<V,E> g) {
 				      Network n = (Network)g;
 				      return(String.valueOf(n.width));
 				  }
 			      });
 	    gmlw.addGraphData("channels", null, "",
 			      new Transformer<Hypergraph<V,E>, String>() {
				  public String transform(Hypergraph<V,E> g) {
 				      Network n = (Network)g;
 				      return(String.valueOf(n.numChannels));
 				  }
 			      });
 	    gmlw.addGraphData("numEdges", null, "",
 			      new Transformer<Hypergraph<V,E>, String>() {
				  public String transform(Hypergraph<V,E> g) {
 				      Network n = (Network)g;
 				      return(String.valueOf(n.getEdgeCount()));
 				  }
 			      });

	    gmlw.save(this, out);
	} catch (IOException e) {
	    System.out.println("Couldn't save network: " + e.getMessage());
	}
    }
}

class BearingSub implements Comparable {

    double bearing;
    Vertex sub;

    public BearingSub(double b, Vertex s) {
        this.bearing = b;
        this.sub = s;
    }

    public int compareTo(Object other) {
        BearingSub otherBS = (BearingSub) other;
        return Double.compare(this.bearing, otherBS.bearing);
    }
}
