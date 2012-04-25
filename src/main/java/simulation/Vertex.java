package simulation;

import java.util.LinkedList;

/**
 *
 * @author Kairat Zhubayev
 */
public class Vertex implements Comparable{
    /**
     * Class Vertex with the following elements:
     * adjacent vertices, vertix number in the array, distance from the start 
     * vertex(-1 at the beginning), the known mark, and a parent link
     */
    
    // Constructor
    public Vertex(int i, Point p){
        vertexNumber = i;
        distance = -1;
        known = false;
        vertices = new LinkedList<ListElement>();
        point = p;
    }
        
    public LinkedList<ListElement> vertices;    // adjacent vertices list
    public int vertexNumber;                    // corresponding vertex number
    public double distance;                     // the distance from a given vertex
    public boolean known;                       // a vertex marked as known or not
    public Vertex path;                         // the path to the initial vertex (a parent link)
    public Point point;                         // corresponding point
    public boolean activeBeams [];
    public int beamsUsedNumber;
    public float inThroughput;
    public float outThroughput;

    /**
     * 
     * @param o is another vertex
     * @return whether two vertices are the same
     */
    public int compareTo(Object o) {
        int index = ((Vertex) o).vertexNumber;
        return this.vertexNumber - index;
    }
}
