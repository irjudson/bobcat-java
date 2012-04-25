package beamscheduling;

import org.apache.commons.collections15.Factory;

class EdgeFactory implements Factory<Edge> {
    int count;
    public Edge create() {
        return new Edge(count++);
    }
}
