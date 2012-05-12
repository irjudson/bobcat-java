package simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 *
 * @author Kairat Zhubayev
 */
public class KNearestNeighborsAlgorithm {

    private Vertex array [];        // The array of vertices
    private Vertex newGraph [];     // The array of vertices
    private ArrayList<ListElement> neighbors [];
    // A heap for the Prim's algorithm
    private int nodeNumber;
    private double squareSide;
    private double threshold = Math.pow(10, 6);     // 1 Mbps
    private int beams;
    private int seed;
    private Throughput throughput;
    private int neighborsNumber;
    private double totalWeight;

    // Constructor
    public KNearestNeighborsAlgorithm(int nodeNumber, int seed, double squareSide, 
            int neighborsNumber, int beams){
        this.nodeNumber = nodeNumber;        
        this.squareSide = squareSide;
        this.neighborsNumber = neighborsNumber;
        this.beams = beams;
        this.seed = seed;
        neighbors = new ArrayList[nodeNumber];        
        this.throughput = new Throughput(beams);
        loadGraph();
    }

    /**
     * Randomly distribute points in the square. Calculate all edge weights.
     * If an edge weight >= threshold, add an edge to the actual graph.
     */
    public void loadGraph(){
        // initialize an array
        array = Utilities.buildRandomGraph(nodeNumber, squareSide, seed);

        // add edges
        for(int i = 0; i < nodeNumber - 1; i++){
            for(int j = i + 1; j < nodeNumber; j++){
                double dist = array[i].point.distance(array[j].point);
                addEdge(i, j, dist, array);
            }
        }
    }
   
    /**
     * add edge between vertices i and j
     * assume graph is not directed
     * @param i is the first vertex
     * @param j is the second vertex
     */
    private void addEdge(int i, int j, double cost, Vertex[] graph){
        if(i < graph.length && j < graph.length && i != j){
            //create a new list element and add it to vertix i's list
            ListElement elem1 = new ListElement(j, cost);
            graph[i].vertices.add(elem1);
            //create a new list element and add it to vertix j's list
            ListElement elem2 = new ListElement(i, cost);
            graph[j].vertices.add(elem2);
        }
    }


    /**
     * find k nearest neighbors
     */
    public void findKNearestNeighbors(){
        for(int i = 0; i < nodeNumber; i++){
            PriorityQueue<ListElement> curr = new
                    PriorityQueue<ListElement>(array[i].vertices.size(), shortestDistanceComparator);
            // add all neighbors to the priority queue
            for(ListElement e: array[i].vertices)
                curr.add(e);
            // save k nearest neighbors
            neighbors[i] = new ArrayList<ListElement>();
            for(int j = 0; j < neighborsNumber; j++){
                if(!curr.isEmpty())
                    neighbors[i].add(curr.poll());
                else
                    break;
            }
            // check
            if(neighbors[i].size() != neighborsNumber)
                System.out.println("The node " + i + " has only " + 
                        neighbors[i].size() + " nearest neighbors!");

            curr.clear();
        }
    }


    /**
     * Iterate through the found MST and "activate" necessary beams. Then
     * calculate new link weights.
     */
    public void constructNewGraph(){
        newGraph = new Vertex[nodeNumber];
        for(int i = 0; i < nodeNumber; i++){
            newGraph[i] = new Vertex(i, array[i].point);
            newGraph[i].activeBeams = new boolean[beams + 1];
        }        

        // iterate through all k nearest neighbors and activate appropriate beams
        for(int i = 0; i < nodeNumber; i++){
            for(int j = 0; j < neighbors[i].size(); j++){
                int endPoint = neighbors[i].get(j).vertexNumber;
                int index1 = array[i].point.beamIndex(beams, array[endPoint].point);
                newGraph[i].activeBeams[index1] = true;

                int index2 = array[endPoint].point.beamIndex(beams, array[i].point);
                newGraph[endPoint].activeBeams[index2] = true;
            }
        }

        // calculate the number of beams used by each vertex
        for(int i = 0; i < nodeNumber; i++){
            int beamsUsed = 0;
            for(int j = 0; j < beams + 1; j++)
                if(newGraph[i].activeBeams[j])
                    beamsUsed++;

            newGraph[i].beamsUsedNumber = beamsUsed;
        }

        // add edges in the new graph        
        totalWeight = 0;
        for(int i = 0; i < nodeNumber - 1; i++){
            for(int j = i + 1; j < nodeNumber; j++){
                int index1 = newGraph[i].point.beamIndex(beams, newGraph[j].point);
                if(!newGraph[i].activeBeams[index1])
                    continue;
                int index2 = newGraph[j].point.beamIndex(beams, newGraph[i].point);
                if(!newGraph[j].activeBeams[index2])
                    continue;

                double dist = newGraph[i].point.distance(newGraph[j].point);
                double weightItoJ = throughput.calculateThroughput(newGraph[i].beamsUsedNumber, dist);
                if(weightItoJ < threshold)
                    continue;
                double weightJtoI = throughput.calculateThroughput(newGraph[j].beamsUsedNumber, dist);
                if(weightJtoI < threshold)
                    continue;

                // create a new list element and add it to vertix i's list
                ListElement elem1 = new ListElement(j, weightItoJ);
                newGraph[i].vertices.add(elem1);
                // create a new list element and add it to vertix j's list
                ListElement elem2 = new ListElement(i, weightJtoI);
                newGraph[j].vertices.add(elem2);

		// Update local throughputs
		newGraph[i].outThroughput += weightItoJ;
		newGraph[i].inThroughput += weightJtoI;
		newGraph[j].outThroughput += weightJtoI;
		newGraph[j].inThroughput += weightItoJ;

                // update total weight
                totalWeight += weightItoJ + weightJtoI;
            }
        }
    }


    public Vertex[] getNewGraph(){
        return newGraph;
    }    

    public ArrayList<ListElement> [] getNeighbors(){
        return neighbors;
    }

    public double getTotalWeight(){
        return totalWeight;
    }

    /**
     * The comparator is used by the PriorityQueue to determine both object
     * ordering and identity. If the comparator returns that two elements are
     * equal, the queue infers they are the same, and it stores only one
     * instance of the element. To prevent losing nodes with equal shortest
     * distances, we must compare the elements themselves
     * (third block in the if statement above).
     */
    private final Comparator<ListElement> shortestDistanceComparator = new Comparator<ListElement>(){
        public int compare(ListElement left, ListElement right)
        {
            double shortestDistanceLeft = left.cost;
            double shortestDistanceRight = right.cost;

            if (shortestDistanceLeft > shortestDistanceRight)
            {
                return +1;
            }
            else if (shortestDistanceLeft < shortestDistanceRight)
            {
                return -1;
            }
            else // equal
            {
                return (right.vertexNumber - left.vertexNumber);
            }
        }
    };


    public static void main(String [] args){
        int nodeNumber = 10;
        int seed = 47;
        double squareSide = 1000;
        int neighborsNumber = 3;
        int beams = 8;
        KNearestNeighborsAlgorithm gr =
                new KNearestNeighborsAlgorithm(nodeNumber, seed, squareSide, neighborsNumber, beams);
        gr.findKNearestNeighbors();
        gr.constructNewGraph();
        Vertex[] topologyGraph = gr.getNewGraph();
        System.out.println("The newly built graph is connected = " +
                Utilities.checkForConnectivity(topologyGraph));
    }

}
