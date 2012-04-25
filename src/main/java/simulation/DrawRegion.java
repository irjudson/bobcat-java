package simulation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author Kairat Zhubayev
 */
public class DrawRegion extends JPanel{
    private JFrame frame;
    private Vertex[] topologyGraph;
    private double squareSide;
    private double total;

    // Constructor
    public DrawRegion(Vertex[] topologyGraph, double squareSide, String frameName, double total){
        this.topologyGraph = topologyGraph;
        this.squareSide = squareSide;
        this.total = total;

        frame = new JFrame(frameName);
        frame.setBackground(Color.WHITE);
        int size = 450;
        frame.setSize(size, size);
        frame.getContentPane().add(this);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    @Override
    public void paintComponent(Graphics g){
        Graphics2D g2 = (Graphics2D)g;
        int width = frame.getSize().width;
        int height = frame.getSize().height;
        
        // draw points
        g2.setColor(Color.RED);
        int[] xCoor = new int[topologyGraph.length];
        int[] yCoor = new int[topologyGraph.length];
        for(int i = 0; i < topologyGraph.length; i++){
            xCoor[i] = (int) Math.round(topologyGraph[i].point.x * width / (1.1 * squareSide));
            yCoor[i] = (int) Math.round(topologyGraph[i].point.y * height / (1.1 * squareSide));
            // to make it look like a typical xy plane
            yCoor[i] = (int) (height / 1.1 - yCoor[i]);
            g2.fillOval(xCoor[i] - 3, yCoor[i] - 3, 6, 6);
            
            g2.setColor(Color.GREEN);
            g2.drawString("" + i, xCoor[i] + 5, yCoor[i]);
            g2.setColor(Color.RED);
        }

        // draw edges
        g2.setColor(Color.BLACK);
        for(int i = 0; i < topologyGraph.length; i++){
            for(ListElement elem: topologyGraph[i].vertices){
                g2.drawLine(xCoor[i], yCoor[i], xCoor[elem.vertexNumber], yCoor[elem.vertexNumber]);
            }
        }

        // write label
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Total: "+Double.toString(total), 200, 425);
    }
}
