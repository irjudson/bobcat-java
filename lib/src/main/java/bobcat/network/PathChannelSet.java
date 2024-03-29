package bobcat.network;

import java.util.ArrayList;
import java.util.TreeSet;

public class PathChannelSet {

    public ArrayList<Edge> path;
    public PathCS pathCS;

    public PathChannelSet() {
        this.path = new ArrayList();
        this.pathCS = new PathCS();
    }

    public PathChannelSet(PathChannelSet other) {
        this.path = (ArrayList<Edge>) other.path.clone();
        this.pathCS = new PathCS();
        this.pathCS.selected = (ArrayList<TreeSet<LinkChannel>>) other.pathCS.selected.clone();
        this.pathCS.throughput = other.pathCS.throughput;
    }

    public String toString() {
        String out = "";
        for (Object o : path) {
            out += ((Edge) o).toString();
            out += ", ";
        }
        return (out);
    }

    public ArrayList<Edge> getPath() {
        return this.path;
    }

    public PathCS getPathCS() {
        return this.pathCS;
    }
}
