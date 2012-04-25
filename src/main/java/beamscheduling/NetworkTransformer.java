package beamscheduling;

import java.util.*;

import java.awt.Dimension;
import java.awt.geom.Point2D;

import org.apache.commons.collections15.Transformer;

class NetworkTransformer implements Transformer<Vertex, Point2D> {
    Dimension dim;
    double width;
    double height;
    int startOffset;
        
    public NetworkTransformer(Dimension dim, double width, double height) {
        this.dim = dim;
        this.width = width;
        this.height = height;
    }
        
    public Point2D transform(Vertex node) {
        double scale_x = (this.dim.getWidth() * 0.95) / this.width;
        double scale_y = (this.dim.getHeight() * 0.95) / this.height;
        double new_x = scale_x * node.location.getX();
        double new_y = scale_y * node.location.getY();
        return new Point2D.Double(new_x, new_y);
    }
}
