package bobcat.network;

import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

import java.util.*;

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
    public boolean[][][] interferes;

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
    public static NetworkGenerator getGenerator(int numRelays, int numSubscribers, int sectors, double width, double height, 
                                                Random random, int theta, double meanq, double slotlen, int channels, double prob) {

        NetworkGenerator gen = new NetworkGenerator(new NetworkFactory(width, height, theta, channels),
                                                    new VertexFactory(width, height, sectors, meanq, random),
                                                    new EdgeFactory(), numRelays, numSubscribers, width, height, random);
        thetaSet[0] = theta;
        meanQueueLength = meanq;
        timeslotLength = slotlen;
        numChannels = channels;
        channelProb = prob;
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
                                                Random random, int channels, double prob) {
        NetworkGenerator gen = new NetworkGenerator(new NetworkFactory(width, 
                                                                       height, 
                                                                       channels),
                                                    new VertexFactory(width, height, random),
                                                    new EdgeFactory(), relays, subscribers, width, height, random);
        numChannels = channels;
        channelProb = prob;
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
