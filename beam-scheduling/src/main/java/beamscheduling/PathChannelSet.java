package beamscheduling;

import java.util.*;

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
//        this.pathCS.selected = new ArrayList();
//        for (TreeSet<LinkChannel> t : other.pathCS.selected) {
//            this.pathCS.selected.add((TreeSet<LinkChannel>) t.clone());
//        }
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
//        ArrayList<Edge> path = new ArrayList<Edge>();
//        for(Object o: path) {
//            path.add(((EdgeChannelSet)o).edge);
//        }
        return this.path;
    }
}
