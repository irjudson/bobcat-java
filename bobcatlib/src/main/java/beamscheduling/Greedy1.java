package beamscheduling;

import java.util.*;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

class Greedy1 {

    Network network;

    public Greedy1(Network<Vertex, Edge> network) {
        this.network = network;
    }

    public double solve() {
        HashMap firstLoop = new HashMap();
        int numVertices = network.getVertexCount();

        for (int i = 0; i < network.subList.length; i++) {
            Vertex s = network.subList[i];
            s.preferredRelay = null;
        }

        // Loop 1
        for (int i = 0; i < network.relayList.length; i++) {
            Vertex r = network.relayList[i];
            r.allocatedChannels = 0;
            int theta = Network.thetaSet[0];
            VertexValue[] vv = new VertexValue[network.subList.length];
            for (int j = 0; j < network.subList.length; j++) {
                Vertex s = network.subList[j];
                double value;
                if (s.preferredRelay == null) {
                    value = s.queueLength * Math.min(s.queueLength, r.calculateThroughput(theta, s) * network.timeslotLength);
                } else {
                    value = s.queueLength * Math.min(s.queueLength, r.calculateThroughput(theta, s) * network.timeslotLength)
                            - s.queueLength * Math.min(s.queueLength, s.preferredRelay.calculateThroughput(theta, s) * network.timeslotLength);
                }
                vv[j] = new VertexValue(s, value);
            }
            Arrays.sort(vv);
            int allocatedChannels = 0;
            for (int n = 0; n < vv.length; n++) {
                if (vv[n].value > 0 && allocatedChannels < network.numChannels) {
                    vv[n].vertex.preferredRelay = r;
                    allocatedChannels++;
                }
            }
        }

        // find beam sets
        for (int i = 0; i < network.relayList.length; i++) {
            Vertex r = network.relayList[i];
            r.allocatedChannels = 0;
            double bestBeamSetValue = 0.0;
            for (int k = 0; k < Network.thetaSet.length; k++) {
                int theta = Network.thetaSet[k];
                for (int l = 0; l < network.beamSet[i][k].length; l++) {
                    HashSet<Vertex> beamSet = network.beamSet[i][k][l];
                    double beamSetVal = 0.0;
                    for (Vertex v : beamSet) {
                        if (v.preferredRelay == r) {
                            beamSetVal += v.queueLength * Math.min(v.queueLength, r.calculateThroughput(theta, v) * network.timeslotLength);
                        }
                    }
                    if (beamSetVal >= bestBeamSetValue) {
                        r.bestK = k;
                        r.bestL = l;
                        bestBeamSetValue = beamSetVal;
                    }
                }
            }
        }


        for (int j = 0; j < network.subList.length; j++) {
            Vertex s = network.subList[j];
            if (s.preferredRelay != null) {
                Vertex r = s.preferredRelay;
                int rIndex = 0;
                for (int i = 0; i < network.relayList.length; i++) {
                    if (r == network.relayList[i]) {
                        rIndex = i;
                    }
                }
                HashSet<Vertex> beamSet = network.beamSet[rIndex][r.bestK][r.bestL];
                if (beamSet.contains(s)) {
                    r.allocatedChannels++;
                } else {
                    s.preferredRelay = null;
                }
            }
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
                if (!bs.contains(s)) {
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

//System.out.println("Solved the greedy problem, overall throughput = " + overallThroughput);

        return objectiveVal;
    }
}
