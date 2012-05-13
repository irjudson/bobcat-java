package bobcat.network;

import org.apache.commons.collections15.Factory;

public class NetworkFactory implements Factory<Network> {
    public double width;
    public double height;
    public int theta;
    public int channels;

    public NetworkFactory(double width, double height, int theta, int channels)
        {
            this.width = width;
            this.height = height;
            this.theta = theta;
            this.channels = channels;
        }

    public NetworkFactory(double width, double height, int channels)
        {
            this.width = width;
            this.height = height;
            this.channels = channels;
            this.theta = 360;
        }

    public Network create() {
        return new Network(this.width, this.height, this.theta, this.channels);
    }
}
