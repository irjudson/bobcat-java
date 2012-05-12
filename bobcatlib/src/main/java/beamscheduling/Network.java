package beamscheduling;

import java.util.*;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

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
    public static NetworkGenerator getGenerator(int numRelays, int numSubscribers, int sectors, double width, double height, long seed, int theta, double meanq, double slotlen, int channels) {
        NetworkGenerator gen = new NetworkGenerator(new NetworkFactory(width, height, theta, channels),
                                                    new VertexFactory(width, height, sectors, meanq),
                                                    new EdgeFactory(), numRelays, numSubscribers, width, height);
        gen.setSeed(seed);
        thetaSet[0] = theta;
        meanQueueLength = meanq;
        timeslotLength = slotlen;
        numChannels = channels;
        return (gen);
    }

    public static NetworkGenerator getGenerator(int relays, int subscribers, 
                                                double width, double height, 
                                                long seed, int channels) {
        NetworkGenerator gen = new NetworkGenerator(new NetworkFactory(width, 
                                                                       height, 
                                                                       channels),
                                                    new VertexFactory(width, height),
                                                    new EdgeFactory(), relays, subscribers, width, height);
        gen.setSeed(seed);
        numChannels = channels;
        return (gen);
    }

    public Vertex randomRelay() {
        Vector<V> v = new Vector<V>(this.getVertices());
        return((Vertex)v.get(this.random.nextInt(v.size())));
    }

    public void calculateBeamSets() {
        int numRelays = relayList.length;
        int numSubs = subList.length;
        this.beamSet = new HashSet[numRelays][this.thetaSet.length][];

        for (int i = 0; i < relayList.length; i++) {
            Vertex relay = relayList[i];

            //System.out.println("computing beam sets for relay " + i);

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
                    //System.out.println("start bearing: " + sortedSubs[start].bearing);
                    while (endBearing - sortedSubs[start].bearing <= this.thetaSet[k]) {
                        //System.out.println("end bearing: " + sortedSubs[end].bearing);
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
                        //System.out.println("beamSet[" + i + "][" + k + "] adding " + nextSet);
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
