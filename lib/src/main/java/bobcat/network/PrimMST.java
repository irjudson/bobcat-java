package bobcat.network;

import bobcat.network.*;

import java.util.*;
import java.util.Collections;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.algorithms.shortestpath.PrimMinimumSpanningTree;

/**
 * For the input Graph, creates a MinimumSpanningTree
 * using a variation of Prim's algorithm.
 * 
 * @author Tom Nelson - tomnelson@dev.java.net
 *
 * @param <V> the vertex type
 * @param <E> the edge type
 */
public class PrimMST<V,E> 
  extends PrimMinimumSpanningTree<V,E> 
  implements Transformer<Graph<V,E>,Graph<V,E>> {
  
  /**
   * Creates an instance which generates a minimum spanning tree assuming constant edge weights.
   */
  public PrimMST(Factory<? extends Graph<V,E>> factory) {
    super(factory);
  }

  /**
   * Creates an instance which generates a minimum spanning tree using the input edge weights.
   */
  public PrimMST(Factory<? extends Graph<V,E>> factory, Transformer<E, Double> weights) {
    super(factory, weights);
  }
     
  protected V findRoot(Graph<V,E> graph) {
    if (graph.getVertexCount() > 0) {
      Vertex retr = null;
      retr = (Vertex)graph.getVertices().iterator().next();
      for(Object o : graph.getVertices()) {
        Vertex v = (Vertex)o;
        if (v.id < retr.id) {
          retr = v;
        }
      }
      System.out.println("Next Node: "+retr);
      return((V)retr);
    } else {
      return null;
    }
  } 
}