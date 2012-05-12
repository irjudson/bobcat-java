package linear;

import ilog.concert.*;
import ilog.cplex.*;
import java.util.Random;
import simulation.*;

/**
 *
 * @author Kairat Zhubayev
 */
public class MaxTotalWeightLP {

    private int nodeNumber;
    private double squareSide;
    private int beams;
    private int seed;
    private Throughput throughput;
    private Vertex vertices[];        // The array of vertices
    private double totalWeight;
    private double cplexTotal;

    // Constructor
    public MaxTotalWeightLP(int nodeNumber, int seed, double squareSide, int beams) {
        this.nodeNumber = nodeNumber;
        this.squareSide = squareSide;
        this.beams = beams;
        this.seed = seed;
        this.throughput = new Throughput(beams);
        loadGraph();
    }

    /**
     * Randomly distribute points in the square. 
     */
    public void loadGraph() {
        // initialize an array
        vertices = Utilities.buildRandomGraph(nodeNumber, squareSide, seed);
    }

    /**
     * The ILP formulation
     */
    public void run() {
        int sourceVertex = 0;
        //int destinationVertex = nodeNumber - 1;
        double threshold = Math.pow(10, 6);

        // c[u][v][j] = transmission rates from u to v, assuming u uses exactly j antenna sectors
        double c[][][] = new double[nodeNumber][nodeNumber][beams + 1];
        for (int i = 0; i < nodeNumber; i++) {
            for (int j = 0; j < nodeNumber; j++) {
                if (i == j) // the same node
                {
                    continue;
                }

                double distance = vertices[i].point.distance(vertices[j].point);
                for (int k = 1; k < beams + 1; k++) {
                    c[i][j][k] = throughput.calculateThroughput(k, distance);
                }
            }
        }

        // sector[u][v]: the sector of u that node v falls in
        int sector[][] = new int[nodeNumber][nodeNumber];
        for (int i = 0; i < nodeNumber; i++) {
            for (int j = 0; j < nodeNumber; j++) {
                if (i == j) {
                    continue;
                }

                sector[i][j] = vertices[i].point.beamIndex(beams, vertices[j].point);
            }
        }

        // create an ILP
        try {
            IloCplex cplex = new IloCplex();

            // x[u][v][j]: u transits v and uses exactly j antenna sectors
            IloNumVar[][][] x = new IloNumVar[nodeNumber][nodeNumber][beams + 1];
            for (int u = 0; u < nodeNumber; u++) {
                for (int v = 0; v < nodeNumber; v++) {
                    for (int j = 1; j < beams + 1; j++) {
                        // I. 0 <= x[u][v][j] <= 1
                        if (u != v) {
                            x[u][v][j] = cplex.numVar(0, 1, "x(" + u + ")(" + v + ")(" + j + ")");
                        }

                        // if c[u][v][j] = 0, set x[u][v][j] to zero
                        if (c[u][v][j] < threshold) {
                            cplex.addEq(0, x[u][v][j]);
                        }
                    }
                }
            }

            // s[u][k]: u uses antenna sector k
            IloIntVar[][] s = new IloIntVar[nodeNumber][beams + 1];
            for (int u = 0; u < nodeNumber; u++) {
                for (int k = 1; k < beams + 1; k++) {
                    // II. 0 <= s[u][k] <= 1
                    s[u][k] = cplex.intVar(0, 1, "s(" + u + ")(" + k + ")");
                }
            }

            // f[u][v]: the amount of flow on the edge (u, v)
            IloNumVar[][] f = new IloNumVar[nodeNumber][nodeNumber];
            for (int u = 0; u < nodeNumber; u++) {
                for (int v = 0; v < nodeNumber; v++) {
                    if (u != v) {
                        f[u][v] = cplex.numVar(0, 1, "f(" + u + ")(" + v + ")");
                    }
                }
            }

            // constraints III, IV, and VI
            for (int u = 0; u < nodeNumber; u++) {
                for (int v = 0; v < nodeNumber; v++) {
                    if (u == v || c[u][v][1] < threshold) {
                        continue;
                    }

                    // expr = sum x[u][v] over j
                    IloLinearNumExpr expr = cplex.linearNumExpr();
                    for (int j = 1; j < beams + 1; j++) {
                        // IV. x[u][v][j] <= s[u][sector(u, v)]
                        int sectorUtoV = sector[u][v];
                        cplex.addLe(x[u][v][j], s[u][sectorUtoV]);

                        expr.addTerm(1, x[u][v][j]);
                    }

                    // III. sum x[u][v] over j <= 1
                    cplex.addLe(expr, 1);

                    // VI. f[u][v] <= sum x[u][v] over j
                    IloLinearNumExpr expr2 = cplex.linearNumExpr();
                    expr2.addTerm(1.0 / (nodeNumber), f[u][v]);
                    cplex.addLe(expr2, expr);
                }
            }

            // Brendan added new approach to constraint 5:
            IloNumVar[][] z = new IloNumVar[nodeNumber][beams + 1];
            for (int v = 0; v < nodeNumber; v++) {
                for (int j = 1; j < beams + 1; j++) {
                    z[v][j] = cplex.numVar(0, Double.MAX_VALUE, "z(" + v + ")(" + j + ")");
                }
            }
            //int xx = 1;
            for (int i = 0; i < Math.pow(2.0, beams); i++) {
                int j = i;
                int k = 1;
                int n = 0;
                int[] element = new int[beams];
                while (j > 0) {
                    if ((j % 2) == 1) {
                        element[n++] = k;
                    }
                    j /= 2;
                    k++;
                }
                if (n > 1) {
                    for (int v = 0; v < nodeNumber; v++) {
                        IloLinearNumExpr rhs = cplex.linearNumExpr(n);
                        for (int l = 0; l < n; l++) {
                            //System.out.print(element[l] + ",");
                            rhs.addTerm(-1, s[v][element[l]]);
                        }
                        //System.out.println(element[n - 1] + "}} sectors_used[v,k];");
                        cplex.addLe(z[v][n - 1], rhs);
                    }
                }
            }
            for (int u = 0; u < nodeNumber; u++) {
                for (int v = 0; v < nodeNumber; v++) {
                    if (u == v || c[u][v][1] < threshold) {
                        continue;
                    }
                    for (int j = 1; j < beams + 1; j++) {
                        cplex.addLe(x[u][v][j], z[u][j]);
                    }
                }
            }

            // VII. vF[v] <= 1/n, n = |V|
            double vFAmount = 1.0 / nodeNumber;

            // VIII. sum f[s][v] over v - sum f[v][s] over v = 1 (Brendan revised)
            IloLinearNumExpr sourceFlow = cplex.linearNumExpr(vFAmount);
            for (int v = 0; v < nodeNumber; v++) {
                if (v == sourceVertex) {
                    continue;
                }

                if (c[sourceVertex][v][1] >= threshold) {
                    sourceFlow.addTerm(1, f[sourceVertex][v]);
                }
                // Brendan added:

                if (c[v][sourceVertex][1] >= threshold) {
                    sourceFlow.addTerm(-1, f[v][sourceVertex]);
                }
            }
            cplex.addEq(1, sourceFlow);

            // sum f[u][v] over u = sum f[v][w] over w
            for (int v = 0; v < nodeNumber; v++) {
                if (v == sourceVertex) {
                    continue;
                }

                IloLinearNumExpr incFlow = cplex.linearNumExpr();
                for (int u = 0; u < nodeNumber; u++) {
                    if (u != v && c[u][v][1] >= threshold) {
                        incFlow.addTerm(1, f[u][v]);
                    }
                }

                IloLinearNumExpr outFlow = cplex.linearNumExpr(vFAmount);
                for (int w = 0; w < nodeNumber; w++) {
                    if (w != v && c[v][w][1] >= threshold) {
                        outFlow.addTerm(1, f[v][w]);
                    }
                }

                cplex.addEq(incFlow, outFlow);
            }

            // new constaint added on the meeting: no unidirectional links
            for (int u = 0; u < nodeNumber - 1; u++) {
                for (int v = u + 1; v < nodeNumber; v++) {
                    IloLinearNumExpr sumUtoV = cplex.linearNumExpr();
                    IloLinearNumExpr sumVtoU = cplex.linearNumExpr();
                    for (int j = 1; j < beams + 1; j++) {
                        sumUtoV.addTerm(1, x[u][v][j]);
                        sumVtoU.addTerm(1, x[v][u][j]);
                    }

                    // sum x[u][v] over j = sum x[v][u] over j
                    cplex.addEq(sumUtoV, sumVtoU);
                }
            }


            // maximize the following expression: sum c[u][v][j] * x[u][v][j]
            IloLinearNumExpr maximizeExpr = cplex.linearNumExpr();
            for (int u = 0; u < nodeNumber; u++) {
                for (int v = 0; v < nodeNumber; v++) {
                    if (u == v) {
                        continue;
                    }
                    for (int j = 1; j < beams + 1; j++) {
                        //if(c[u][v][1] >= threshold)
                        maximizeExpr.addTerm(c[u][v][j], x[u][v][j]);
                    }
                }
            }

            // solve the problem
            IloObjective obj = cplex.maximize(maximizeExpr);
            cplex.add(obj);
            cplex.setOut(null);
//            cplex.exportModel("MaxTotalWeight.lp");
            if (cplex.solve()) {
                cplexTotal = cplex.getObjValue();
//                System.out.println("Max total weight LP = " + cplexTotal);
//                System.out.println("Solution status = " + cplex.getStatus());

                // figure out the antenna pattern assignment
                for (int u = 0; u < nodeNumber; u++) {
                    vertices[u].activeBeams = new boolean[beams + 1];
                    vertices[u].beamsUsedNumber = 0;

                    for (int k = 1; k < beams + 1; k++) {
                        double temp = cplex.getValue(s[u][k]);
                        if (temp > 0.9) {
                            vertices[u].activeBeams[k] = true;
                            vertices[u].beamsUsedNumber++;
                        }
                    }
                }

                // let's check everything
                for (int u = 0; u < nodeNumber; u++) {
                    for (int v = 0; v < nodeNumber; v++) {
                        for (int j = 1; j < beams + 1; j++) {
                            if (u == v) {
                                continue;
                            }

                            double value = cplex.getValue(x[u][v][j]);
                            if (value > 0.1) {
                                if (value - sector[u][v] > 0.1) {
                                    throw new RuntimeException("1");
                                }
                                if (vertices[u].beamsUsedNumber != j) {
                                    // this can happen if both transmission rates are equal
                                    double diff = c[u][v][vertices[u].beamsUsedNumber] - c[u][v][j];
                                    if (Math.abs(diff) > threshold) {
                                        System.out.print("vertices[u].beamsUsedNumber = "
                                                + vertices[u].beamsUsedNumber);
                                        System.out.println(", while j = " + j);
                                        System.out.println("c[u][v][vertices[u].beamsUsedNumber] = "
                                                + c[u][v][vertices[u].beamsUsedNumber]);
                                        System.out.println("c[u][v][j] = " + c[u][v][j]);
                                        throw new RuntimeException("2");
                                    }
                                }
                                if (c[u][v][vertices[u].beamsUsedNumber] < threshold) {
                                    throw new RuntimeException("3");
                                }
                                if (c[v][u][vertices[v].beamsUsedNumber] < threshold) {
                                    throw new RuntimeException("4");
                                }
                            }
                        }
                    }
                }


                // build the topology
                totalWeight = 0;
                for (int i = 0; i < nodeNumber - 1; i++) {
                    for (int j = i + 1; j < nodeNumber; j++) {
                        int index1 = sector[i][j];
                        if (!vertices[i].activeBeams[index1]) {
                            continue;
                        }
                        int index2 = sector[j][i];
                        if (!vertices[j].activeBeams[index2]) {
                            continue;
                        }

                        double weightItoJ = c[i][j][vertices[i].beamsUsedNumber];
                        if (weightItoJ < threshold) {
                            continue;
                        }

                        double weightJtoI = c[j][i][vertices[j].beamsUsedNumber];
                        if (weightJtoI < threshold) {
                            continue;
                        }

                        // create a new list element and add it to vertix i's list
                        ListElement elem1 = new ListElement(j, weightItoJ);
                        vertices[i].vertices.add(elem1);

                        // create a new list element and add it to vertix j's list
                        ListElement elem2 = new ListElement(i, weightJtoI);
                        vertices[j].vertices.add(elem2);

                        // Update local throughputs
                        double i_to_j = c[i][j][vertices[i].beamsUsedNumber] * cplex.getValue(x[i][j][vertices[i].beamsUsedNumber]);
                        double j_to_i = c[j][i][vertices[j].beamsUsedNumber] * cplex.getValue(x[j][i][vertices[j].beamsUsedNumber]);

                        vertices[i].outThroughput += i_to_j;
                        vertices[i].inThroughput += j_to_i;
                        vertices[j].outThroughput += j_to_i;
                        vertices[j].inThroughput += i_to_j;

                        // update total weight
//                        System.out.println("Optimal total Weight = " + totalWeight + " + " + weightItoJ + " + " + weightJtoI);
                        totalWeight += weightItoJ + weightJtoI;
                    }
                }
            }

        } catch (IloException ex) {
            ex.printStackTrace();
        }
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    public double getCplexTotal() {
        return cplexTotal;
    }

    public Vertex[] getGraph() {
        return vertices;
    }

    public static void main(String[] args) {
        int nodeNumber = 10;
        double squareSide = 30000;
        int beams = 8;

        int size = 15;
        int[] seeds = new int[size];
        double[] cplex = new double[size];
        double[] graph = new double[size];

        int counter = 0;
        Random random = new Random();
        while (counter < size) {
            int seed = random.nextInt(1000000) + 1;
            MaxTotalWeightLP gr = new MaxTotalWeightLP(nodeNumber, seed, squareSide, beams);
            gr.run();
            Vertex[] topologyGraph = gr.getGraph();

            if (Utilities.checkForConnectivity(topologyGraph)) {
                seeds[counter] = seed;
                cplex[counter] = gr.getCplexTotal();
                graph[counter] = gr.getTotalWeight();
            }

            counter++;
        }

        System.out.println("\n\nSeed\tgraph total\tcplex total");
        for (int i = 0; i < size; i++) {
            System.out.println(seeds[i] + "\t" + graph[i] + "\t"
                    + cplex[i]);
        }
    }
}
