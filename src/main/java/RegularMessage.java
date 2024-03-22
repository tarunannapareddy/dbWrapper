import java.io.Serializable;

public class RegularMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    public Integer seqNumber;
    public Object message;

    public Integer localSeqNumber;

    public int source;

    public RegularMessage(Integer seqNumber,Integer localSeqNumber, Object message, int source) {
        this.seqNumber = seqNumber;
        this.message = message;
        this.localSeqNumber = localSeqNumber;
        this.source = source;
    }
}
