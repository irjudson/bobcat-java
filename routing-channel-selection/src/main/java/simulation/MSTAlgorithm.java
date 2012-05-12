package simulation;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 *
 * @author Kairat Zhubayev
 */
public class MSTAlgorithm {
    
    private Vertex array [];        // The array of vertices
    private Vertex newGraph [];     // The array of vertices
    HashSet<Edge> MST = new HashSet<Edge>();
    // A heap for the Prim's algorithm
    private PriorityQueue<Vertex> unknownNodes;    
    private int nodeNumber;
    private double squareSide;
    private int beams;
    private int seed;
    private Throughput throughput;
    private double threshold = Math.pow(10, 6);     // 1 Mbps
    private double totalWeight;

    // Constructor
    public MSTAlgorithm(int nodeNumber, int seed, double squareSide, int beams){
        this.nodeNumber = nodeNumber;        
        this.squareSide = squareSide;
        this.beams = beams;
        this.seed = seed;
        unknownNodes = new PriorityQueue<Vertex>(nodeNumber, shortestDistanceComparator);
        this.throughput = new Throughput(beams);
        loadGraph();
    }
    
    /**
     * Randomly distribute points in the square. Calculate all edge weights.
     * If an edge weight >= threshold, add an edge to the actual graph.
     */
    public void loadGraph(){        
        //initialize an array
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
     * find minimum spanning tree
     * use the Prims's algorithm to solve this problem
     */
    public void buildMinimumSpanningTree(){
        int startVertex = 0;
        //System.out.println("The start vertex for MST is " + array[startVertex].point);
        // begin Prim's algorithm
        array[startVertex].distance = 0;
        unknownNodes.add(array[startVertex]);
        
        // while there are some unknown nodes, keep going
        while (!unknownNodes.isEmpty()){
            // get the node with the shortest distance
            Vertex u = extractMin();
            // mark as known
            u.known = true;
            // relax neighbors
            for(ListElement v: u.vertices){
                if(!array[v.vertexNumber].known){
                    // if a shorter distance exists
                    if ((array[v.vertexNumber].distance == -1) || (array[v.vertexNumber].distance > v.cost)){
                        // update a distance (using Prim's algorithm!)
                        array[v.vertexNumber].distance = v.cost;
                        // update a path
                        array[v.vertexNumber].path = u;
                        // add a new vertex to the queue
                        unknownNodes.add(array[v.vertexNumber]);
                    }
                }   
            }
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

        // find all edges of the MST
        HashSet<Edge> edges = new HashSet<Edge>();
        for(int i = 0; i < nodeNumber; i++){
            if(array[i].path != null){
                Edge e = new Edge(i, array[i].path.vertexNumber);
                edges.add(e);
            }
        }

        if(edges.size() != nodeNumber - 1){
            System.out.println("Number of nodes = " + nodeNumber +
                    ", number of edges in the MST = " + edges.size());
            throw new RuntimeException("MST must have |V| - 1 edges!");
        }
        MST = edges;

        // iterate through all edges and activate appropriate beams
        Iterator iter = edges.iterator();
        while(iter.hasNext()){
            Edge e = (Edge) iter.next();
            int index1 = array[e.endPoint1].point.beamIndex(beams, array[e.endPoint2].point);
            newGraph[e.endPoint1].activeBeams[index1] = true;

            int index2 = array[e.endPoint2].point.beamIndex(beams, array[e.endPoint1].point);
            newGraph[e.endPoint2].activeBeams[index2] = true;
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
//                System.out.println("MST total Weight = " + totalWeight + " + " + weightItoJ + " + " + weightJtoI);
                totalWeight += weightItoJ + weightJtoI;
            }
        }
    }
    

    public Vertex[] getNewGraph(){
        return newGraph;
    }

    public HashSet<Edge> getMST(){
        return MST;
    }

    public double getTotalWeight(){
        return totalWeight;
    }

    /**
     * Find the vertex with minimum distance from unknown vertices
     * @return the head of the queue
     */
    private Vertex extractMin(){
        return unknownNodes.poll();
    }
    
    
    /**
     * The comparator is used by the PriorityQueue to determine both object
     * ordering and identity. If the comparator returns that two elements are
     * equal, the queue infers they are the same, and it stores only one
     * instance of the element. To prevent losing nodes with equal shortest
     * distances, we must compare the elements themselves
     * (third block in the if statement above).
     */
    private final Comparator<Vertex> shortestDistanceComparator = new Comparator<Vertex>(){
        public int compare(Vertex left, Vertex right)
        {
            double shortestDistanceLeft = left.distance;
            double shortestDistanceRight = right.distance;

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
                   
}
