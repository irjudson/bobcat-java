package bobcat.simulation;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import org.apache.commons.collections15.Transformer;

import javax.swing.*;
import java.awt.*;

import bobcat.network.Network;
import bobcat.network.Edge;
import bobcat.network.Vertex;
import bobcat.network.NetworkTransformer;

public class Draw {
    
    Network network;
    Integer width, height;
    String name;
    JFrame jf;

    public Draw(Network network, int width, int height, String name) {
        this.network = network;
        this.height = height;
        this.width = width;
        this.name = name;
    }

    public void draw() {
        Transformer<Edge, Stroke> edgeDraw = new Transformer<Edge, Stroke>() {
            public Stroke transform(Edge e) {
                float weight = (float)(e.capacity/Math.pow(10, 7));
                return new BasicStroke(weight, BasicStroke.CAP_BUTT, 
                                       BasicStroke.JOIN_MITER, 10.0f, 
                                       null, 0.0f);
            }
        };
        Transformer<Vertex, Paint> vertexPaint = new Transformer<Vertex, Paint>() {

            public Paint transform(Vertex v) {
                if (v.type == 0) {
                    return Color.YELLOW;
                } else if (v.type == 1) {
                    return Color.CYAN;
                } else if (v.type == 2) {
                    return Color.GREEN;
                } else if (v.type == 3) {
                    return Color.RED;
                } else if (v.type == 4) {
                    return Color.BLUE;
                } else {
                    return Color.BLACK;
                }
            }
        };

        Transformer<Edge, Paint> edgePaint = new Transformer<Edge, Paint>() {
            public Paint transform(Edge e) {
                if (e.type == 0) {
                    return Color.BLACK;
                } else if (e.type == 1) {
                    return Color.CYAN;
                } else if (e.type == 2) {
                    return Color.GREEN;
                } else if (e.type == 3) {
                    return Color.RED;
                } else if (e.type == 4) {
                    return Color.YELLOW;
                } else {
                    return Color.BLACK;
                }
            }
        };

        System.setProperty("com.apple.mrj.application.apple.menu.about.name", 
                           "Wireless Mesh Network Simulation");
        jf = new JFrame(name);
        final Dimension layoutSize = new Dimension(width, height);
        final Layout layout = new StaticLayout(network, new NetworkTransformer(layoutSize, network.width, network.height), layoutSize);
        final VisualizationViewer vv = new VisualizationViewer(layout, layoutSize);

        final float scale_x = (float) ((layoutSize.getWidth() * 0.95) / network.width);
        final float scale_y = (float) ((layoutSize.getHeight() * 0.95) / network.height);
    
        vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        vv.getRenderContext().setEdgeStrokeTransformer(edgeDraw);
        vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller());
        vv.getRenderContext().setEdgeDrawPaintTransformer(edgePaint);
        vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);

        jf.getContentPane().add(vv);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.pack();
        jf.setVisible(true);
    }

    void repaint() {
        this.jf.repaint();
    }
}
