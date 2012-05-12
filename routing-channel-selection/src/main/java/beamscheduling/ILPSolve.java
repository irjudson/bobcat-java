/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package beamscheduling;

//import java.util.*;
//import edu.uci.ics.jung.graph.Graph;
//import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import ilog.concert.*;
import ilog.cplex.*;

/**
 *
 * @author bmumey
 */
public class ILPSolve {

    Network network;
    double threshold = 0.0001;

    public ILPSolve(Network<Vertex, Edge> network) {
        this.network = network;
    }

    public double solve() {

        // create an ILP
        Vertex[] relay = network.relayList;
        int numRelays = network.relayList.length;
        int numSubscribers = network.subList.length;
        Vertex[] sub = network.subList;
        int numThetas = Network.thetaSet.length;
        double r[][][] = new double[numRelays][numSubscribers][numThetas];

        for (int i = 0; i < numRelays; i++) {
            for (int j = 0; j < numSubscribers; j++) {
                for (int k = 0; k < numThetas; k++) {
                    r[i][j][k] = relay[i].calculateThroughput(network.thetaSet[k], sub[j]) * network.timeslotLength;
                }
            }
        }

        try {
            IloCplex cplex = new IloCplex();

            // x[i][j][k]: relay i transmits to subscriber j using beam theta_k
            IloIntVar[][][] x = new IloIntVar[numRelays][numSubscribers][numThetas];
            for (int i = 0; i < numRelays; i++) {
                for (int j = 0; j < numSubscribers; j++) {
                    for (int k = 0; k < numThetas; k++) {
                        // I. 0 <= x[u][v][j] <= 1
                        x[i][j][k] = cplex.intVar(0, 1, "x(" + i + ")(" + j + ")(" + k + ")");


                        // if c[i][j][k] = 0, set x[i][j][k] to zero
                        if (r[i][j][k] < threshold) {
                            cplex.addEq(0, x[i][j][k]);
                        }
                    }
                }
            }

            // s[i][k][l]: relay i uses beam theta_k and picks beam set l
            IloIntVar[][][] s = new IloIntVar[numRelays][numThetas][];
            for (int i = 0; i < numRelays; i++) {
                for (int k = 0; k < numThetas; k++) {
                    int numSets = network.beamSet[i][k].length;
                    s[i][k] = new IloIntVar[numSets];
                    for (int l = 0; l < numSets; l++) {
                        s[i][k][l] = cplex.intVar(0, 1, "s(" + i + ")(" + k + ")(" + l + ")");
                    }
                }
            }


            for (int j = 0; j < numSubscribers; j++) {
                IloLinearNumExpr expr = cplex.linearNumExpr();
                for (int i = 0; i < numRelays; i++) {
                    for (int k = 0; k < numThetas; k++) {
                        expr.addTerm(1, x[i][j][k]);
                    }
                }
                cplex.addLe(expr, 1.0);
            }

            for (int i = 0; i < numRelays; i++) {
                IloLinearNumExpr expr = cplex.linearNumExpr();
                for (int j = 0; j < numSubscribers; j++) {
                    for (int k = 0; k < numThetas; k++) {
                        expr.addTerm(1, x[i][j][k]);
                    }
                }
                cplex.addLe(expr, network.numChannels);
            }

            for (int i = 0; i < numRelays; i++) {
                IloLinearNumExpr expr = cplex.linearNumExpr();
                for (int k = 0; k < numThetas; k++) {
                    int numSets = network.beamSet[i][k].length;
                    for (int l = 0; l < numSets; l++) {
                        expr.addTerm(1, s[i][k][l]);
                    }
                }
                cplex.addLe(expr, 1);
            }

            for (int i = 0; i < numRelays; i++) {
                for (int j = 0; j < numSubscribers; j++) {
                    IloLinearNumExpr rhs = cplex.linearNumExpr();
                    for (int k = 0; k < numThetas; k++) {
                        int numSets = network.beamSet[i][k].length;
                        for (int l = 0; l < numSets; l++) {
                            if (network.beamSet[i][k][l].contains(sub[j])) {
                                rhs.addTerm(1, s[i][k][l]);
                            }
                        }
                        IloLinearNumExpr lhs = cplex.linearNumExpr();
                        lhs.addTerm(1, x[i][j][k]);
                        cplex.addLe(lhs, rhs);
                    }
                }
            }

            IloNumVar[] y = new IloNumVar[numSubscribers];
            for (int j = 0; j < numSubscribers; j++) {
                y[j] = cplex.numVar(0, sub[j].queueLength, "y(" + j + ")");
                cplex.addLe(y[j], sub[j].queueLength);
                IloLinearNumExpr expr = cplex.linearNumExpr();
                for (int i = 0; i < numRelays; i++) {
                    for (int k = 0; k < numThetas; k++) {
                        expr.addTerm(r[i][j][k], x[i][j][k]);
                    }
                }
                cplex.addLe(y[j], expr);
            }

            // maximize the following 
            IloLinearNumExpr maximizeExpr = cplex.linearNumExpr();
            for (int j = 0; j < numSubscribers; j++) {
                maximizeExpr.addTerm(sub[j].queueLength, y[j]);
            }



            // solve the problem
            IloObjective obj = cplex.maximize(maximizeExpr);
            cplex.add(obj);
            cplex.setOut(null);
//            cplex.exportModel("MaxTotalWeight.lp");
            if (cplex.solve()) {
                double cplexTotal = cplex.getObjValue();


//                for (int i = 0; i < numRelays; i++) {
//                    for (int j = 0; j < numSubscribers; j++) {
//                        IloLinearNumExpr rhs = cplex.linearNumExpr();
//                        for (int k = 0; k < numThetas; k++) {
//                            if (cplex.getValue(x[i][j][k]) > threshold) {
//                                System.out.println("x[" + relay[i] + "," + sub[j] + "," + k + "] = 1");
//                            }
//                        }
//                    }
//                }
//                for (int j = 0; j < numSubscribers; j++) {
//                    System.out.println("sub " + sub[j] + " objective " + sub[j].queueLength * cplex.getValue(y[j]));
//                }
                return cplexTotal;
            }

        } catch (IloException ex) {
            ex.printStackTrace();
        }

        return -1.0; // error
    }
}