package beamscheduling;

import java.text.DecimalFormat;

/**
 *
 * @author Kairat Zhubayev
 */
public class Point implements Comparable{
    public double x;
    public double y;
    private double epsilon = 0.0000001;

    public Point(double x, double y){
        this.x = x;
        this.y = y;
    }

    /**
     * Given this point and another point p, we try to figure out which beam
     * should we use for the point p.
     * @param beams is the number of beams used (e.g. 4)
     * @param p is another point
     * @return the beam index in which point p lies for this point
     */
    public int beamIndex(int numberOfBeams, Point p){
        // if two points coincide
        if (this.compareTo(p) == 0)
            return -1;
        
        double angle = -1;

        // special cases: x coordinates are equal, and we don't want to divide by zero
        if (Math.abs(p.x - this.x) < epsilon) {
            if (p.y > this.y)
                angle = Math.PI / 2.0;
            else
                angle = 3.0 * Math.PI / 2.0;
        }
        
        if (p.x >= this.x && p.y >= this.y) {
            // I quadrant
            angle = Math.atan((p.y - this.y) / (p.x - this.x));
        } else if (p.x <= this.x && p.y >= this.y) {    
            // II quadrant
            angle = Math.PI - Math.atan(Math.abs(p.y - this.y) / Math.abs(p.x - this.x));
        } else if (p.x <= this.x && p.y <= this.y) {    
            // III quadrant
            angle = Math.PI + Math.atan(Math.abs(p.y - this.y) / Math.abs(p.x - this.x));
        } else {   
            // IV quadrant
            angle = 2 * Math.PI - Math.atan(Math.abs(p.y - this.y) / Math.abs(p.x - this.x));
        }

        return ((int) (1 + Math.floor(angle / (2.0 * Math.PI / numberOfBeams))));
    }

    /**
     *
     * @param p is some point
     * @return distance between p and this point
     */
    public double distance(Point p) {
        return Math.sqrt(Math.pow(x - p.x, 2) + Math.pow(y - p.y, 2));
    }

    /**
     *
     * @param o is some point
     * @return whether this point coincides with o
     */
    public int compareTo(Object o) {
        double x2 = ((Point) o).x;
        double y2 = ((Point) o).y;

        if(Math.abs(x - x2) > epsilon){
            if(x < x2)
                return -1;
            else
                return 1;
        }
        else if(Math.abs(y - y2) > epsilon){
            if(y < y2)
                return -1;
            else
                return 1;
        }

        return 0;
    }

    /**
     *
     * @return a string representation of this point
     */
    @Override
        public String toString(){
        return "x = " + Point.roundTwoDecimals(x) +
            ", y = " + Point.roundTwoDecimals(y);
    }

    /**
     *
     * @param num is any real number
     * @return num rounded to two decimal places
     */
    public static double roundTwoDecimals(double num) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Double.valueOf(twoDForm.format(num));
    }


    /**
     * A small test for the class
     * @param args
     */
    public static void main(String[] args){
        Point p1 = new Point(0, 3);
        Point p2 = new Point(4, 0);
        Point p3 = new Point(7, 0);
        System.out.println(p1.distance(p2));
        System.out.println(p2.distance(p3));

        Point origin = new Point(0, 0);
        System.out.println("Beam index (should be 1) = " + origin.beamIndex(4, new Point(0, 1)));
        System.out.println("Beam index (should be 1) = " + origin.beamIndex(4, new Point(1, 1)));
        System.out.println("Beam index (should be 2) = " + origin.beamIndex(4, new Point(-1, 1)));
        System.out.println("Beam index (should be 3) = " + origin.beamIndex(4, new Point(-1, -1)));
        System.out.println("Beam index (should be 4) = " + origin.beamIndex(4, new Point(1, -1)));        
    }
}
