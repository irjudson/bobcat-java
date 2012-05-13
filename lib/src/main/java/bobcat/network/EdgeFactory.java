package bobcat.network;

import org.apache.commons.collections15.Factory;

public class EdgeFactory implements Factory<Edge> {
    int count;
    public Edge create() {
        return new Edge(count++);
    }
}
