package simulation;

/**
 *
 * @author Kairat Zhubayev
 */
public class Edge implements Comparable{
    /**
     * An element of the tree
     */

    // Constructor
    public Edge(int end1, int end2){
        // make sure that endPoint1 is less than endPoint2
        if(end1 > end2){
            endPoint1 = end2;
            endPoint2 = end1;
        }
        else if(end1 < end2){
            endPoint1 = end1;
            endPoint2 = end2;
        }        
    }

    public int endPoint1;                  // first end point of an edge
    public int endPoint2;                  // second end point of an edge    

    public int compareTo(Object o) {
        int end1 = ((Edge) o).endPoint1;
        int end2 = ((Edge) o).endPoint2;

        if(this.endPoint1 < end1)
            return 1;
        else{
            if(this.endPoint1 > end1)
                return -1;
            else{
                if(this.endPoint2 < end2)
                    return 1;
                else if(this.endPoint2 > end2)
                    return -1;
            }
        }

        return 0;
    }
}
