package dbPojo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class TokenMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public Integer global_seq =-1;

    public Integer min_aru;

    public Integer min_aru_port;

    public Integer sender_port;

    public Integer count =0;

    public Set<Integer> voteSet = new HashSet<>();
    public int term =0;

    public int candidate =-1;

    public Long election_start =null;

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
