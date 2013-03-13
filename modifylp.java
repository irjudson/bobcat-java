
package test_cplex;

import java.io.*;
import java.util.*;
import ilog.concert.*;
import ilog.cplex.*;


public class TestILP {

    public static void main(String[] args) {
        solveILP();
    }

    public static void solveILP() {

        try {
            IloCplex cplex = new IloCplex();

            IloIntVar x = cplex.intVar(0, 10, "x");
            IloIntVar y = cplex.intVar(0, 10, "y");
            IloIntVar z = cplex.intVar(0, 10, "z");

            IloLinearNumExpr expr = cplex.linearNumExpr();

            expr.addTerm(1, x);
            expr.addTerm(1, y);
            expr.addTerm(1, z);

            IloObjective obj = cplex.maximize(expr);
            cplex.add(obj);
            cplex.setOut(null);

            if (cplex.solve()) {
                System.out.println("objective = " + cplex.getObjValue() + "\n");
            }

            IloRange r = cplex.addEq(x, 5);
            if (cplex.solve()) {
                System.out.println("objective = " + cplex.getObjValue() + "\n");
            }

            cplex.remove(r);
            if (cplex.solve()) {
                System.out.println("objective = " + cplex.getObjValue() + "\n");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
