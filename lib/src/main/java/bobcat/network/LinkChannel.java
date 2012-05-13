package bobcat.network;

public class LinkChannel implements Comparable<LinkChannel> {

    int pathLinkIndex;
    int channel;

    public LinkChannel(int index, int c) {
        pathLinkIndex = index;
        channel = c;
    }

    public String toString() {
        return "(" + pathLinkIndex + "," + channel + ")";
    }

    public int compareTo(LinkChannel other) {
        if (pathLinkIndex == other.pathLinkIndex) {
            return (channel - other.channel);
        } else {
            return (pathLinkIndex - other.pathLinkIndex);
        }
    }
}
