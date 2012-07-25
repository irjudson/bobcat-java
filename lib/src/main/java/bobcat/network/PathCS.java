package bobcat.network;

import java.util.ArrayList;
import java.util.TreeSet;

public class PathCS {

    public ArrayList<TreeSet<LinkChannel>> selected;
    double throughput;

    PathCS() {
        selected = new ArrayList<TreeSet<LinkChannel>>();
        throughput = Double.MAX_VALUE; // initial unlimited before using any links
    }

    public ArrayList<TreeSet<LinkChannel>> getSelected() {
        return selected;
    }
    public String toString() {
        String out = new String();
        out += "/" + throughput + "/ ";
        for (Object o : selected) {
            for (Object o2 : (TreeSet) o) {
                out += " [" + (LinkChannel) o2 + " ]";
            }
        }
        return (out);
    }

    void print() {
        for (TreeSet<LinkChannel> ts : selected) {
            for (LinkChannel lc : ts) {
                System.out.println(lc);
            }
        }
    }
}
