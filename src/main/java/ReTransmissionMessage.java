import java.io.Serializable;

public class ReTransmissionMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public int sender_port;

    public int global_seq;

    public ReTransmissionMessage(int sender_port, int global_seq) {
        this.sender_port = sender_port;
        this.global_seq = global_seq;
    }
}
