package simulation;

/**
 *
 * @author Kairat Zhubayev
 */
public class ListElement {
    /**
     * Serves as an element of the adjacent vertices list and has following
     * elements: corresponding vertex number and the link weight
     */

    // Constructor
    public ListElement(int vert, double linkCost){
        vertexNumber = vert;
        cost = linkCost;
    }

    public int vertexNumber;               // corresponding vertex number
    public double cost;                    // the cost of the link
}
