package beamscheduling;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Collection;
import java.util.Vector;
import java.util.TreeMap;
import edu.uci.ics.jung.graph.Graph;

public class Vertex implements Comparable {
    public enum Type { GATEWAY, RELAYSTATION, SUBSCRIBERSTATION };
    public int     id;
    public int     sectors;
    public Point2D location;        
    // maps int sector -> array of vertices.
    public HashMap sectorMap;
    public int     activeSectors;
    public int     type;  // 0 - Gateway, 1 - RS, 2 - SS
    public float   inThroughput;
    public float   outThroughput;
    public Vertex  preferredRelay;
    public double  bestBearing;
    public int     bestK, bestL; // Brendan added
    public int     allocatedChannels;
    public double  queueLength;
    private double gainReceiver = Math.pow(10, 2.0 / 10);       // 2 dB
    private double frequency = 2.4 * Math.pow(10, 9);           // 2.4 Ghz
    private double c = 3.0 * Math.pow(10, 8);                   // Speed of light
    private double lambda = c / frequency;
    public Integer interferenceChannel;
    public Boolean isMarked = false;
    public TreeMap rcsPaths;  

    public int compareTo(Object otherVertex) throws ClassCastException {
        if(!(otherVertex instanceof Vertex))
            throw new ClassCastException("A Vertex object is expected.");
        int otherId = ((Vertex)otherVertex).id;
        return this.id - otherId;
    }

    /**
     * @param id the id of the vertex
     * @param sectors the number of sectors this node has
     * @param x the x location of this vertex
     * @param y the y location of this vertex
     * @return Vertex a new vertex.
     **/
    public Vertex(int id, int sectors, double x, double y, double queueLength){
        this.id = id;
        this.sectors = sectors;
        this.sectorMap = new HashMap(sectors);
        this.queueLength = queueLength;
        for(int i=0; i<sectors; i++) {
            this.sectorMap.put(i, new HashSet<Vertex>());
        }
        location = new Point2D.Double(x,y);
    }

    /**
     * @return string the string form of this vertex to print.
     **/
    public String toString() {
        return Integer.toString(id);
    }

    /**
     * Given this vertex and another vertex p, we try to figure out which beam
     * should we use for the vertex p.
     * @param v is another vertex
     * @return the beam index in which vertex p lies for this vertex
     */
    public int findSector(Vertex v){
        if(this == v)
            return -1;
        Point2D p = v.location;
        double angle = -1.0;
        double this_x = this.location.getX();
        double this_y = this.location.getY();
        double other_x = p.getX();
        double other_y = p.getY();
        double diff_x = Math.abs(p.getX() - this.location.getX());
        double diff_y = Math.abs(p.getY() - this.location.getY());
        double dxdy = Math.atan(diff_y / diff_x);

        // special cases: x coordinates are equal, and we don't want to divide by zero
        if (diff_x < 1.0) {
            if (other_y > this_y)
                angle = Math.PI / 2.0;
            else
                angle = 3.0 * Math.PI / 2.0;
        }
        
        if (other_x >= this_x && other_y >= this_y) {
            // Quadrant I
            angle = dxdy;
        } else if (other_x <= this_x && other_y >= this_y) {
            // Quadrant II
            angle = Math.PI - dxdy;
        } else if (other_x <= this_x && other_y <= this_y) {
            // Quadrant III
            angle = Math.PI + dxdy;
        } else {
            // Quadrant IV
            angle = 2 * Math.PI - dxdy;
        }

        double ratio = angle / (2.0 * Math.PI / this.sectors);
        return (int)Math.floor(ratio);
    }

    /**
     *
     * @param beams is the number of beams being used.
     * @
     **/
    public void computeConnectivity(Graph g) {
        int sector;
        HashSet sectorsUsed = new HashSet<Integer>();
        HashSet<Edge> toBeRemoved = new HashSet<Edge>();

        // Get the sectors needed
        for(Vertex v: (Collection<Vertex>)g.getNeighbors(this)) {
            sectorsUsed.add(findSector(v));
        }

        this.activeSectors = sectorsUsed.size();

        for(Vertex v: (Collection<Vertex>)g.getNeighbors(this)) {
            int sectorUsed = findSector(v);
            double dist = this.location.distance(v.location);
            double range = calculateRange(this.activeSectors);
            if (dist < range) {
                sector = findSector(v);
                HashSet<Vertex> nodeSet = (HashSet<Vertex>)this.sectorMap.get(sector);
                nodeSet.add(v);
                this.sectorMap.put(sector, nodeSet);
            } else {
                toBeRemoved.add((Edge)g.findEdge(this, v));
            }
        }

        for(Edge e: toBeRemoved) {
            g.removeEdge(e);
        }
    }

    /**
     *
     * @param sectors is the number of sectors activated by the transmitter
     * @return the max distance at which a receiver can be reached
     */
    public double calculateRange(int activeSectors) {
        // Coverage in degrees
        double covered = activeSectors * (360.0 / this.sectors);

        // Transmitter Gain in dB
        double gainTransmitter = Math.pow(10, (2 + 10 * Math.log10(360.0 / covered)) / 10.0);
        // Transmitter 
        double range = this.gainReceiver * gainTransmitter * Math.pow(this.lambda, 2);
        range /= Math.pow(4 * Math.PI, 2);
        range /= Math.pow(10, -12.6);
        range = Math.sqrt(range);

        return range;
    }

    /**
     *
     * @param sectors is the number of sectors activated by the transmitter
     * @param distance is the distance b/n transmitter and receiver
     * @return the user throughput
     */
   public double calculateThroughput(int sectors, double distance){
       double covered = sectors * (360.0 / activeSectors);
       double gainTransmitter = Math.pow(10, (2 + 10 * Math.log10(360.0 / covered)) / 10.0);
       double pathLoss = gainReceiver * gainTransmitter * Math.pow(lambda, 2);
       pathLoss /= Math.pow(4 * Math.PI * distance, 2);
       // in dB
       pathLoss = 10 * Math.log10(pathLoss);
       // take absolute value
       pathLoss = Math.abs(pathLoss);

       double weight = 0;
       if(pathLoss > 126)
           weight = 0;                     // 0 Mb/s
       else if (pathLoss > 121.5)
           weight = 10 * Math.pow(10, 6);  // 10 Mb/s
       else if (pathLoss > 118.75)
           weight = 20 * Math.pow(10, 6);  // 20 Mb/s
       else if (pathLoss > 114.5)
           weight = 30 * Math.pow(10, 6);  // 30 Mb/s
       else if (pathLoss > 113)
           weight = 40 * Math.pow(10, 6);  // 40 Mb/s
       else
           weight = 45 * Math.pow(10, 6);  // 45 Mb/s

       return weight;
   }

    public double distanceTo(Vertex v) {
        double distance = Math.pow(this.location.getX()/1000.0 - v.location.getX()/1000.0, 2);
        distance += Math.pow(this.location.getY()/1000.0 - v.location.getY()/1000.0, 2);
        return Point.roundTwoDecimals(Math.sqrt(distance));
    }

    public double calculateThroughput(Vertex v) {
       double distance = distanceTo(v);
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
    }

   /**
     *
     * @param covered is the width of the beam
     * @param distance is the distance b/n transmitter and receiver
     * @return the user throughput
     */
   public double calculateThroughput(int covered, Vertex v){
       double distance = location.distance(v.location);
       //       double gainTransmitter = Math.pow(10, (2 + 10 * Math.log10(360.0 / covered)) / 10.0);
       double gainTransmitter = gainReceiver;
       double pathLoss = gainReceiver * gainTransmitter * Math.pow(lambda, 2);
       pathLoss /= Math.pow(4 * Math.PI * distance, 2);
       // in dB
       pathLoss = 10 * Math.log10(pathLoss);
       // take absolute value
       pathLoss = Math.abs(pathLoss);

       double weight = 0;
       if(pathLoss > 126)
           weight = 0;                     // 0 Mb/s
       else if (pathLoss > 121.5)
           weight = 10 * Math.pow(10, 6);  // 10 Mb/s
       else if (pathLoss > 118.75)
           weight = 20 * Math.pow(10, 6);  // 20 Mb/s
       else if (pathLoss > 114.5)
           weight = 30 * Math.pow(10, 6);  // 30 Mb/s
       else if (pathLoss > 113)
           weight = 40 * Math.pow(10, 6);  // 40 Mb/s
       else
           weight = 45 * Math.pow(10, 6);  // 45 Mb/s

       System.out.println("Distance: " + distance + " TP: " + weight);

       return weight;
   }

    public double getBearing(Vertex other) {
        double x1 = this.location.getX();
        double y1 = this.location.getY();
        double x2 = other.location.getX();
        double y2 = other.location.getY();
        double length = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
        double angleRadians = Math.acos((y2 - y1) / length); // in radians
        double angleDegrees = Math.toDegrees(angleRadians);

        if (x2 > x1) {
            return angleDegrees;
        }
        return (360.0 - angleDegrees);
    }
    
    public boolean containsInBeam(Vertex node, int theta) {
        double rBearing = this.getBearing(node);
        double lowerBound = rBearing - (theta/2);
        double upperBound = rBearing + (theta/2);
        // Check if node is within the beam
        if(this.bestBearing >= lowerBound && this.bestBearing <= upperBound) {
            return true;
        }
        return false;
    }
}
