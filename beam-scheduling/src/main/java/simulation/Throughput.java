package simulation;

/**
 *
 * @author Kairat Zhubayev
 */
public class Throughput {

    private double gainReceiver = Math.pow(10, 2.0 / 10);       // 2 dB
    private double frequency = 5.8 * Math.pow(10, 9);   // 5.8 Ghz
    private double c = 3.0 * Math.pow(10, 8);
    private int beamsNumber;
    private double lambda;

    public Throughput(int beams) {
        this.beamsNumber = beams;
        lambda = c / frequency;
    }

    /**
     *
     * @param sectors is the number of sectors activated by the transmitter
     * @return the max distance at which a receiver can be reached
     */
    public double calculateRange(int sectors) {
        double covered = sectors * (360.0 / beamsNumber);
        double gainTransmitter = Math.pow(10, (2 + 10 * Math.log10(360.0 / covered)) / 10.0);

        double range = gainReceiver * gainTransmitter * Math.pow(lambda, 2);
        range /= Math.pow(4 * Math.PI, 2);
        range /= Math.pow(10, -12.6);
        range = Math.sqrt(range);

        return range;
    }

    /**
     *
     * @param sectors is the number of sectors activated by the transmitter
     * @param distance is the distance b/n transmitter and receiver
     * @return the user throughput
     */
   public double calculateThroughput(int sectors, double distance){
       double covered = sectors * (360.0 / beamsNumber);
       double gainTransmitter = Math.pow(10, (2 + 10 * Math.log10(360.0 / covered)) / 10.0);
       double pathLoss = gainReceiver * gainTransmitter * Math.pow(lambda, 2);
       pathLoss /= Math.pow(4 * Math.PI * distance, 2);
       // in dB
       pathLoss = 10 * Math.log10(pathLoss);
       // take absolute value
       pathLoss = Math.abs(pathLoss);

       double weight = 0;
       if(pathLoss > 126)
           weight = 0;
       else if (pathLoss > 121.5)
           weight = 10 * Math.pow(10, 6);  // 10 Mb/s
       else if (pathLoss > 118.75)
           weight = 20 * Math.pow(10, 6);  // 20 Mb/s
       else if (pathLoss > 114.5)
           weight = 30 * Math.pow(10, 6);  // 30 Mb/s
       else if (pathLoss > 113)
           weight = 40 * Math.pow(10, 6);  // 40 Mb/s
       else
           weight = 45 * Math.pow(10, 6);  // 45 Mb/s

//        System.out.println("Sectors = " + sectors + ", distance = " + distance +
//                ", path loss = " + pathLoss + " dB, weight = " + weight);

       return weight;
   }

    public static void main(String[] args) {
        Throughput thr = new Throughput(8);
        double dist = 1800;
        System.out.println("Distance = " + dist + ", throughput = "
                + thr.calculateThroughput(8, dist));

        dist = 2200;
        System.out.println("Distance = " + dist + ", throughput = "
                + thr.calculateThroughput(8, dist));

        dist = 3600;
        System.out.println("Distance = " + dist + ", throughput = "
                + thr.calculateThroughput(8, dist));

        dist = 4900;
        System.out.println("Distance = " + dist + ", throughput = "
                + thr.calculateThroughput(8, dist));

        dist = 8200;
        System.out.println("Distance = " + dist + ", throughput = "
                + thr.calculateThroughput(8, dist));

        System.out.println("number of sectors\trange");
        for (int i = 1; i <= 8; i++) {
            System.out.println(i + "\t\t\t" + thr.calculateRange(i));
        }
    }
}
