package simulation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author Kairat Zhubayev
 */
public class DrawMST extends JPanel{
    private JFrame frame;
    private Vertex[] topologyGraph;
    private double squareSide;
    private HashSet<Edge> MST;

    // Constructor
    public DrawMST(Vertex[] topologyGraph, double squareSide, HashSet<Edge> MST){
        this.topologyGraph = topologyGraph;
        this.squareSide = squareSide;
        this.MST = MST;

        frame = new JFrame("The Minimum Spanning Tree");
        frame.setBackground(Color.WHITE);
        int size = 900;
        frame.setSize(size, size);
        frame.getContentPane().add(this);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    @Override
    public void paintComponent(Graphics g){
        Graphics2D g2 = (Graphics2D)g;
        //System.out.println("width = " + frame.getSize().width + ", height = " + frame.getSize().height);
        int width = frame.getSize().width;
        int height = frame.getSize().height;

        // draw points
        g2.setColor(Color.RED);
        int[] xCoor = new int[topologyGraph.length];
        int[] yCoor = new int[topologyGraph.length];
        for(int i = 0; i < topologyGraph.length; i++){
            xCoor[i] = (int) Math.round(topologyGraph[i].point.x * width / (1.1 * squareSide));
            yCoor[i] = (int) Math.round(topologyGraph[i].point.y * height / (1.1 * squareSide));
            g2.fillOval(xCoor[i] - 2, yCoor[i] - 2, 4, 4);
        }

        // draw edges
        g2.setColor(Color.BLACK);
        Iterator iter = MST.iterator();
        while(iter.hasNext()){
            Edge e = (Edge) iter.next();
            g2.drawLine(xCoor[e.endPoint1], yCoor[e.endPoint1],
                    xCoor[e.endPoint2], yCoor[e.endPoint2]);
        }

        g2.drawString("The start vertex", xCoor[0], yCoor[0]);

        //g2.fillRect(0, 0, 100, 50);
    }
}
