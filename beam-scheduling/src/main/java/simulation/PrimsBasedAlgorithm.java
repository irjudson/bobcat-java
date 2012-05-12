package simulation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

/**
 *
 * @author Kairat Zhubayev
 */
public class PrimsBasedAlgorithm {
    private Vertex vertices [];        // The array of vertices           
    private int nodeNumber;
    private double squareSide;
    private int beams;
    private int seed;
    private Throughput throughput;
    private double threshold = Math.pow(10, 6);     // 1 Mbps
    private double totalWeight;

    // Constructor
    public PrimsBasedAlgorithm(int nodeNumber, int seed, double squareSide, int beams){
        this.nodeNumber = nodeNumber;
        this.squareSide = squareSide;
        this.beams = beams;
        this.seed = seed;        
        this.throughput = new Throughput(beams);
        loadGraph();
    }

    /**
     * Randomly distribute points in the square. Calculate all edge weights.
     * If an edge weight >= threshold, add an edge to the actual graph.
     */
    public void loadGraph(){
        // initialize an array
        vertices = Utilities.buildRandomGraph(nodeNumber, squareSide, seed);
    }    


    /**     
     * use the Prims's like algorithm to solve this problem
     */
    public void run(){
        int startVertex = 0;
        HashSet<Vertex> unknownNodes = new HashSet<Vertex>();
        unknownNodes.add(vertices[startVertex]);

        // the maximum distance at which a node can be reached using i sectors
        double range[] = new double[beams + 1];
        for(int i = 1; i < beams + 1; i++)
            range[i] = throughput.calculateRange(i);

        // the main algorithm
        int markedVertexNumber = 0;
        while(!unknownNodes.isEmpty()){            
            double vertexMaxGain = 0;
            Vertex bestVertex = null;
            int vertexBestSectorsNumber = 0;
            boolean[] vertexBestSectors = new boolean[beams + 1];

            // iterate through all vertices that can be reached from the connected component
            Iterator iter = unknownNodes.iterator();
            while(iter.hasNext()){
                Vertex vert = (Vertex) iter.next();
                // local variables for the current vertex
                double maxGain = 0;
                int bestSectorsNumber = 0;
                boolean[] bestSectors = new boolean[beams + 1];
                boolean canBeConnectedToTree = false;

                // activate j sectors at a time to find the max utility value
                for(int j = 1; j <= beams; j++){
                    // an array to store number of vertices reachable through each sector
                    Sector[] count = new Sector[beams + 1];
                    count[0] = new Sector(0, Integer.MIN_VALUE);
                    for(int k = 1; k < count.length; k++)
                        count[k] = new Sector(k);
                    boolean[] connectToTree = new boolean[beams + 1];

                    // iterate through all vertices to find the sector they belong to
                    for(int i = 0; i < vertices.length; i++){
                        if(vert.compareTo(vertices[i]) == 0)
                            continue;

                        int index = vert.point.beamIndex(beams, vertices[i].point);
                        double distance = vert.point.distance(vertices[i].point);
                        if(distance <= range[j]){
                            count[index].neighborCount++;
                            // check if a vertex belongs to the connected component
                            if(vertices[i].known)
                                connectToTree[index] = true;
                        }
                    }

                    // find j best sectors
                    Arrays.sort(count);
                    boolean[] chosenSectors = new boolean[beams + 1];
                    int selected = 0;
                    // pick a marked sector
                    int counter = beams;
                    while(counter > 0){
                        int index = count[counter].sectorNumber;                        

                        if(connectToTree[index] && index != 0){
                            chosenSectors[index] = true;
                            selected++;
                            canBeConnectedToTree = true;
                            break;
                        }

                        counter--;
                    }
                    // if we can't connect to the tree, should we drop this case?
                    if(markedVertexNumber > 0 && !canBeConnectedToTree)
                        continue;

                    // pick the rest of the sectors
                    counter = beams;
                    while(selected < j){
                        int index = count[counter].sectorNumber;                                                
                        if(!chosenSectors[index] && index != 0){
                            chosenSectors[index] = true;
                            selected++;
                        }

                        counter--;
                    }

                    // now calculate the gain
                    double gain = 0;
                    // iterate through all vertices
                    for(int i = 0; i < vertices.length; i++){
                        if(vert.compareTo(vertices[i]) == 0)
                            continue;

                        // if the vertex is in the chosen sector
                        int index = vert.point.beamIndex(beams, vertices[i].point);
                        if(!chosenSectors[index])
                            continue;

                        // calculate the throughput and add it to the gain
                        double distance = vert.point.distance(vertices[i].point);
                        double weight = throughput.calculateThroughput(j, distance);
                        gain += weight;
                    }

                    // compare gain to the best so far
                    if(gain > maxGain){
                        maxGain = gain;
                        bestSectorsNumber = j;
                        bestSectors = chosenSectors;
                    }
                }

                // small modification - divide gain by (1 + # of nodes left out)
                int nodesLeftOut = 0;
                for(int i = 0; i < vertices.length; i++){
                    if(vert.compareTo(vertices[i]) == 0)
                        continue;

                    int index = vert.point.beamIndex(beams, vertices[i].point);
                    double distance = vert.point.distance(vertices[i].point);
                    // if the node is in the range
                    if(distance <= range[bestSectorsNumber]){
                        // and if the sector is not activated
                        if(!bestSectors[index])
                            nodesLeftOut++;     // then this node is left out
                    }
                }
                // so the gain is divided by (1 + # of nodes left out) here
                maxGain /= 1.0 + nodesLeftOut;

                // compare current vertex to the best so far
                if(maxGain > vertexMaxGain){
                    if(canBeConnectedToTree || markedVertexNumber == 0){
                        vertexMaxGain = maxGain;
                        bestVertex = vert;
                        vertexBestSectorsNumber = bestSectorsNumber;
                        vertexBestSectors = bestSectors;
                    }
                }
            }

            // mark vertex here
            bestVertex.known = true;
            bestVertex.beamsUsedNumber = vertexBestSectorsNumber;
            bestVertex.activeBeams = vertexBestSectors;
            markedVertexNumber++;
            // remove from the list of unmarked nodes
            unknownNodes.remove(bestVertex);
            
            // check
            if(bestVertex.activeBeams[0]){
                System.out.println("The beam sector 0 can't be used!");
                throw new RuntimeException("The beam sector 0 can't be used!");
            }

            // add all vertices that can be reached from this best vertex
            for(int i = 0; i < vertices.length; i++){
                if(bestVertex.compareTo(vertices[i]) == 0 || vertices[i].known)
                    continue;

                int index = bestVertex.point.beamIndex(beams, vertices[i].point);
                if(!bestVertex.activeBeams[index])
                    continue;

                double distance = bestVertex.point.distance(vertices[i].point);
                if(distance <= range[bestVertex.beamsUsedNumber]){
                    if(!unknownNodes.contains(vertices[i])){
                        unknownNodes.add(vertices[i]);
                    }
                }
            }
        }

        // see if all nodes are marked
        if(markedVertexNumber != vertices.length){
            System.out.println("Number of marked vertices = " + markedVertexNumber);
            throw new RuntimeException("Prims-based algorithm: not all nodes are marked");
        }

        // check 
        for(int i = 0; i < nodeNumber; i++){
            int usedSectors = 0;
            for(int j = 1; j < beams + 1; j++){
                if(vertices[i].activeBeams[j])
                    usedSectors++;
            }
            
            if(usedSectors != vertices[i].beamsUsedNumber)
                throw new RuntimeException("Beams used number is wrong!");
        }

        // build the topology
        totalWeight = 0;
        for(int i = 0; i < nodeNumber - 1; i++){
            for(int j = i + 1; j < nodeNumber; j++){
                int index1 = vertices[i].point.beamIndex(beams, vertices[j].point);
                if(!vertices[i].activeBeams[index1])
                    continue;
                int index2 = vertices[j].point.beamIndex(beams, vertices[i].point);
                if(!vertices[j].activeBeams[index2])
                    continue;

                double dist = vertices[i].point.distance(vertices[j].point);
                double weightItoJ = throughput.calculateThroughput(vertices[i].beamsUsedNumber, dist);
                if(weightItoJ < threshold)
                    continue;
                double weightJtoI = throughput.calculateThroughput(vertices[j].beamsUsedNumber, dist);
                if(weightJtoI < threshold)
                    continue;
                // create a new list element and add it to vertix i's list
                ListElement elem1 = new ListElement(j, weightItoJ);
                vertices[i].vertices.add(elem1);

                // create a new list element and add it to vertix j's list
                ListElement elem2 = new ListElement(i, weightJtoI);
                vertices[j].vertices.add(elem2);

		// Update local throughputs
		vertices[i].outThroughput += weightItoJ;
		vertices[i].inThroughput += weightJtoI;
		vertices[j].outThroughput += weightJtoI;
		vertices[j].inThroughput += weightItoJ;

                // update total weight
//                System.out.println("Prim's total Weight = " + totalWeight + " + " + weightItoJ + " + " + weightJtoI);
                totalWeight += weightItoJ + weightJtoI;
            }
        }

    }

   
    public Vertex[] getGraph(){
        return vertices;
    }

    public double getTotalWeight(){
        return totalWeight;
    }

    /**
     * The simple class that surves only one purpose: to have a way to find j
     * best sectors
     */
    private static class Sector implements Comparable{
        // Constructor
        Sector(int sectorNumber){
            this.sectorNumber = sectorNumber;
            this.neighborCount = 0;
        }

        // Constructor
        Sector(int sectorNumber, int count){
            this.sectorNumber = sectorNumber;
            this.neighborCount = count;
        }

        int sectorNumber;
        int neighborCount;

        public int compareTo(Object o) {
            return this.neighborCount - ((Sector) o).neighborCount;
        }
    }


    public static void main(String[] args){
        int nodeNumber = 20;
        double squareSide = 30000;
        int beams = 8;
        System.out.println("The number of nodes = " + nodeNumber);
        System.out.println("The square side = " + squareSide);

//        int seed = 1535;
//        System.out.println("The seed = " + seed);
//
//        PrimsBasedAlgorithm prims = new PrimsBasedAlgorithm(nodeNumber, seed, squareSide, beams);
//        prims.run();
//        Vertex[] primsGraph = prims.getGraph();
//        System.out.println("\n\nThe Prim's-based graph is connected = " +
//                Utilities.checkForConnectivity(primsGraph));
//
//        DrawRegion draw = new DrawRegion(primsGraph, squareSide, "The Prim's-based graph topology");
//        System.out.println("Prim's-based total = " + prims.getTotalWeight());


        int size = 10000;
        int connected = 0;
        int counter = 0;
        Random random = new Random();
        while(counter < size){
            int seed = random.nextInt() + 1;
            try{
                PrimsBasedAlgorithm prims = new PrimsBasedAlgorithm(nodeNumber, seed, squareSide, beams);
                prims.run();
                Vertex[] primsGraph = prims.getGraph();
                if(Utilities.checkForConnectivity(primsGraph))
                    connected++;
            }
            catch(Exception ex){}

            counter++;            
        }

        double percent = 100.0 * connected / size;
        System.out.println("For nodeNumber = " + nodeNumber + ", size = " + size
                + ", square side = " + squareSide
                + ", the graph was connected " + percent + "% of the time.");


        /**
         * For nodeNumber = 10, size = 10000, square side = 30000.0,
         * the graph was connected 47.55% of the time.
         *
         * For nodeNumber = 11, size = 10000, square side = 30000.0,
         * the graph was connected 46.44% of the time.
         *
         * For nodeNumber = 12, size = 10000, square side = 30000.0,
         * the graph was connected 46.33% of the time.
         *
         * For nodeNumber = 15, size = 10000, square side = 30000.0,
         * the graph was connected 48.39% of the time.
         *
         * For nodeNumber = 20, size = 10000, square side = 30000.0,
         * the graph was connected 51.78% of the time.
         */
    }
}
