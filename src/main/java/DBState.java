import java.util.*;

public class DBState {
    public Integer local_seq =0;
    public Integer local_aru =-1;

    public Deque<RegularMessage> requestQueue = new ArrayDeque<>();
    public PriorityQueue<RegularMessage> receivedQueue = new PriorityQueue<RegularMessage>((a,b)->{return a.seqNumber-b.seqNumber;});
    public int[] ports;
    public Map<Integer, RegularMessage> storage = new HashMap();
    public Set<Integer> liveServers;

    public Set<Integer> deadServers;

    public Map<Integer, Long> lastHeartbeatTime = new HashMap<>();
    public Integer global_seq =-1;

    public Map<Integer, String> queryResponse = new HashMap();

    public Map<String, Object> nextTokenMap;

    public int nextPort;

}
