package beamscheduling;

import java.util.*;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

class Greedy2 {

    Network network;

    public Greedy2(Network<Vertex, Edge> network) {
        this.network = network;
    }

    public double solve() {
        HashMap firstLoop = new HashMap();
        int numVertices = network.getVertexCount();

        for (int i = 0; i < network.subList.length; i++) {
            Vertex s = network.subList[i];
            s.preferredRelay = null;
        }

        // Loop 1: Choose the beam sets for each relay
        for (int i = 0; i < network.relayList.length; i++) {
            Vertex r = network.relayList[i];
            r.allocatedChannels = 0;
            double bestBeamSetValue = 0.0;
            for (int k = 0; k < Network.thetaSet.length; k++) {
                int theta = Network.thetaSet[k];
                for (int l = 0; l < network.beamSet[i][k].length; l++) {
                    HashSet<Vertex> beamSet = network.beamSet[i][k][l];
                    VertexValue[] vv = new VertexValue[beamSet.size()];
                    int m = 0;
                    for (Vertex v : beamSet) {
                        double value;
                        if (v.preferredRelay == null) {
                            value = v.queueLength * Math.min(v.queueLength, r.calculateThroughput(theta, v) * network.timeslotLength);
                        } else {
                            value = v.queueLength * Math.min(v.queueLength, r.calculateThroughput(theta, v) * network.timeslotLength)
                                    - v.queueLength * Math.min(v.queueLength, v.preferredRelay.calculateThroughput(theta, v) * network.timeslotLength);
                        }
                        vv[m++] = new VertexValue(v, value);
                    }
                    Arrays.sort(vv);
                    int allocatedChannels = 0;
                    double beamSetValue = 0.0;
                    for (int n = 0; n < vv.length; n++) {
                        if (vv[n].value > 0 && allocatedChannels < network.numChannels) {
                            //vv[n].sub.preferredRelay = r;
                            allocatedChannels++;
                            beamSetValue += vv[n].value;
                        }
                    }
                    if (beamSetValue >= bestBeamSetValue) {
                        r.bestK = k;
                        r.bestL = l;
                        bestBeamSetValue = beamSetValue;
                    }
                }
            }
            HashSet<Vertex> beamSet = network.beamSet[i][r.bestK][r.bestL];
            int theta = Network.thetaSet[r.bestK];
            VertexValue[] vv = new VertexValue[beamSet.size()];
            int m = 0;
            for (Vertex v : beamSet) {
                double value;
                if (v.preferredRelay == null) {
                    value = v.queueLength * Math.min(v.queueLength, r.calculateThroughput(theta, v) * network.timeslotLength);
                } else {
                    value = v.queueLength * Math.min(v.queueLength, r.calculateThroughput(theta, v) * network.timeslotLength)
                            - v.queueLength * Math.min(v.queueLength, v.preferredRelay.calculateThroughput(theta, v) * network.timeslotLength);
                }
                vv[m++] = new VertexValue(v, value);
            }
            Arrays.sort(vv);
            r.allocatedChannels = 0;
            for (int n = 0; n < vv.length; n++) {
                //System.out.println(vv[n].sub + " queuelength = " + vv[n].sub.queueLength + " value = " + vv[n].value);
                if (vv[n].value > 0 && r.allocatedChannels < network.numChannels) {
                    if (vv[n].vertex.preferredRelay != null) {
                        vv[n].vertex.preferredRelay.allocatedChannels--;
                    }
                    vv[n].vertex.preferredRelay = r;
                    //System.out.println(vv[n].sub + " prefers " + r);
                    r.allocatedChannels++;
                }
            }
            //System.out.println("relay " + r + ": best theta " + network.thetaSet[r.bestK] + " best l " + r.bestL);
        }
        for (int j = 0; j < network.subList.length; j++) {
            Vertex s = network.subList[j];
            if (s.preferredRelay == null) {
                VertexValue[] vv = new VertexValue[network.relayList.length];
                for (int i = 0; i < network.relayList.length; i++) {
                    Vertex r = network.relayList[i];
                    HashSet<Vertex> bs = network.beamSet[i][r.bestK][r.bestL];
                    int theta = network.thetaSet[r.bestK];
                    if (bs.contains(s)) {
                        vv[i] = new VertexValue(r, s.queueLength * Math.min(s.queueLength, r.calculateThroughput(theta, s) * network.timeslotLength));
                    } else {
                        vv[i] = new VertexValue(r, 0.0);
                    }
                }
                Arrays.sort(vv);
                for (int i = 0; i < network.relayList.length; i++) {
                    if (vv[i].value > 0.0 && vv[i].vertex.allocatedChannels < network.numChannels) {
                        s.preferredRelay = vv[i].vertex;
                        vv[i].vertex.allocatedChannels++;
                        break;
                    }
                }
            }
        }

        double objectiveVal = 0.0;

        for (int j = 0; j < network.subList.length; j++) {
            Vertex s = network.subList[j];
            if (s.preferredRelay != null) {
                int prefRelayIndex = 0;
                for (int i = 0; i < network.relayList.length; i++) {
                    if (s.preferredRelay == network.relayList[i]) {
                        prefRelayIndex = i;
                    }
                }
                //System.out.println(s + " assigned to " + s.preferredRelay);
                HashSet<Vertex> bs = network.beamSet[prefRelayIndex][s.preferredRelay.bestK][s.preferredRelay.bestL];
                if (! bs.contains(s)) {
                    System.out.println(s + " is NOT contained in beam set of " + s.preferredRelay);
                }
                double obj = s.queueLength * Math.min(s.queueLength, s.preferredRelay.calculateThroughput(network.thetaSet[s.preferredRelay.bestK], s) * network.timeslotLength);
                //System.out.println("sub " + s + " greedy obj val = " + obj);
                objectiveVal += obj;
                Edge e = new Edge();
                e.type = 1;
                e.length = Point.roundTwoDecimals(s.preferredRelay.calculateThroughput(network.thetaSet[s.preferredRelay.bestK], s));
                network.addEdge(e, s, s.preferredRelay);
                firstLoop.put(s, s.preferredRelay);
            }
        }

        // center beams:
        for (int i = 0; i < network.relayList.length; i++) {
            Vertex r = network.relayList[i];
            double min = 360.0;
            double max = 0.0;
            for (int j = 0; j < network.subList.length; j++) {
                Vertex s = network.subList[j];
                if (s.preferredRelay == r) {
                    min = Math.min(min, r.getBearing(s));
                    max = Math.max(max, r.getBearing(s));
                }
            }
            if (max - min <= network.thetaSet[r.bestK]) {
                r.bestBearing = (max + min) / 2.0;
            } else {
                min += 360.0;
                double b = (max + min) / 2.0;
                if (b > 360.0) {
                    b -= 360.0;
                }
                r.bestBearing = b;
            }
        }

        //System.out.println("Solved the greedy problem, overall throughput = " + overallThroughput);
        return objectiveVal;
    }
}

class VertexValue implements Comparable {

    double value;
    Vertex vertex;

    public VertexValue(Vertex s, double v) {
        this.vertex = s;
        this.value = v;
    }

    public int compareTo(Object other) {
        VertexValue otherBS = (VertexValue) other;
        return Double.compare(otherBS.value, this.value); // reversed so it sorts in decreasing order
    }
}
