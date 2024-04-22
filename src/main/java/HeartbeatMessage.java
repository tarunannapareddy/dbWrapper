import java.io.Serializable;

public class HeartbeatMessage implements Serializable {
    int senderPort;
    long timestamp;

    public HeartbeatMessage(int senderPort, long timestamp) {
        this.senderPort = senderPort;
        this.timestamp = timestamp;
    }
}