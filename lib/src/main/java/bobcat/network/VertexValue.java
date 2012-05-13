package bobcat.network;

/**
 * Created with IntelliJ IDEA.
 * User: ivan.judson
 * Date: 5/13/12
 * Time: 10:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class VertexValue implements Comparable {

    double value;
    Vertex vertex;

    public VertexValue(Vertex s, double v) {
        this.vertex = s;
        this.value = v;
    }

    public double getValue() {
        return value;
    }

    public Vertex getVertex() {
        return vertex;
    }

    public int compareTo(Object other) {
        VertexValue otherBS = (VertexValue) other;
        return Double.compare(otherBS.value, this.value); // reversed so it sorts in decreasing order
    }
}
