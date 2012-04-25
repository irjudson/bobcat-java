package beamscheduling;

import java.text.DecimalFormat;
import org.apache.commons.lang.StringUtils;
import java.util.Vector;

public class Edge {

    public int id;
    public double length;
    public double capacity;
    public int type;
    public Double[] channels;
    public Boolean isMarked = false;
    public double weight = 0.0d;

    public Edge(int id) {
        this.id = id;
        this.type = 0;
    }

    public Edge() {
        this.id = -1;
        this.type = 0;
    }

    public double bottleNeckCapacity() {
        double sum = 0.0d;
        for (int i = 0; i < channels.length; i++) {
            if (channels[i] > 0.0d) {
                sum += channels[i];
            }
        }
        return (sum);
    }

    public double bottleNeckWeight() {
        double max = channels.length * 45 * Math.pow(10, 6);
        //return (max - (this.weight * bottleNeckCapacity()));
        return (max - (1.0 * bottleNeckCapacity()));
    }

    public String toString() {
        DecimalFormat dec = new DecimalFormat("##.#");
        String delim = "";
        Vector active = new Vector();
        for (int i = 0; i < channels.length; i++) {
            if (channels[i] > 0.0) {
                active.add(i);
            }
        }
        String c = "{ " + StringUtils.join(active, ", ") + " }";
        return ("[" + Integer.toString(id) + "] " + dec.format(length) + " km " + c);
    }

    double lookupThroughput(int frequency) {
        double distance = this.length;

        if (frequency == 700) {
            if (distance > 68.0) {
                return 0.0;
            } else if (distance > 41.0) {
                return 10 * Math.pow(10, 6);
            } else if (distance > 30.0) {
                return 20 * Math.pow(10, 6);
            } else if (distance > 18.4) {
                return 30 * Math.pow(10, 6);
            } else if (distance > 15.4) {
                return 40 * Math.pow(10, 6);
            } else {
                return 45 * Math.pow(10, 6);
            }
        } else if (frequency == 2400) {
            if (distance > 20.0) {
                return 0.0;
            } else if (distance > 11.8) {
                return 10 * Math.pow(10, 6);
            } else if (distance > 8.6) {
                return 20 * Math.pow(10, 6);
            } else if (distance > 5.3) {
                return 30 * Math.pow(10, 6);
            } else if (distance > 4.5) {
                return 40 * Math.pow(10, 6);
            } else {
                return 45 * Math.pow(10, 6);
            }
        } else if (frequency == 5800) {
            if (distance > 8.2) {
                return 0.0;
            } else if (distance > 4.9) {
                return 10 * Math.pow(10, 6);
            } else if (distance > 3.6) {
                return 20 * Math.pow(10, 6);
            } else if (distance > 2.2) {
                return 30 * Math.pow(10, 6);
            } else if (distance > 1.8) {
                return 40 * Math.pow(10, 6);
            } else {
                return 45 * Math.pow(10, 6);
            }
        }
        return 0.0;
    }
}
