package bobcat.network;

import org.apache.commons.collections15.Factory;

import java.util.Random;

public class VertexFactory implements Factory<Vertex> {

    int count;
    double width;
    double height;
    int sectors;
    double meanQueueLength;
    Random generator;

    public VertexFactory(double width, double height, int sectors, double meanq, long seed)
        {
            this.width = width;
            this.height = height;
            this.sectors = sectors;
            this.meanQueueLength = meanq;
            generator = new Random(seed);
        }
    
    public VertexFactory(double width, double height, long seed)
        {
            this.width = width;
            this.height = height;
            this.sectors = 1;
            this.meanQueueLength = 0;
            generator = new Random(seed);
        }

    public Vertex create() {
        double x = Point.roundTwoDecimals(generator.nextDouble());
        double y = Point.roundTwoDecimals(generator.nextDouble());
        double queueLength = generator.nextDouble() * 2.0 * this.meanQueueLength;
        return new Vertex(count++, this.sectors, x * this.width, y * this.height, queueLength);
    }

    public Vertex create(Vertex root, double max_r) {
        double theta = ((generator.nextDouble() * 360.0) * Math.PI) / 180.0;
        double radius = generator.nextDouble() * max_r;
        double x = root.location.getX() + (radius * Math.cos(theta));
        double y = root.location.getY() + (radius * Math.sin(theta));
        double queueLength = generator.nextDouble() * 2.0 * this.meanQueueLength;
        return new Vertex(count++, this.sectors, x * this.width, y * this.height, queueLength);
    }

    public long setSeed(long seed) {
        generator = new Random(seed);
        return seed;
    }
}
