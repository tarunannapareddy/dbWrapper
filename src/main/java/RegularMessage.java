import java.io.Serializable;

public class RegularMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    public Integer seqNumber;
    public Object message;

    public Integer localSeqNumber;

    public int source;

    public String table;

    public String function;

    public Object input;

    public RegularMessage(Integer seqNumber,Integer localSeqNumber, String table, String function, Object input, int source) {
        this.seqNumber = seqNumber;
        this.table = table;
        this.function = function;
        this.input = input;
        this.localSeqNumber = localSeqNumber;
        this.source = source;
    }
}
