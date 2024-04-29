package dbPojo;

import lombok.Builder;

import java.io.Serializable;
import java.util.List;

@Builder
public class JoinMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public List<Integer> alive_list;

    public List<Integer> dead_list;

    public int candidate;

    public int term;

}
