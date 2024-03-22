import java.io.Serializable;

public class TokenMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public Integer global_seq =-1;

    public Integer min_aru;

    public Integer min_aru_port;

    public Integer sender_port;

    public Integer count =0;

    public TokenMessage(Integer global_seq, Integer min_aru, Integer min_aru_port, Integer sender_port, Integer count) {
        this.global_seq = global_seq;
        this.min_aru = min_aru;
        this.min_aru_port = min_aru_port;
        this.sender_port = sender_port;
        this.count = count;
    }

    public TokenMessage() {
    }
}
