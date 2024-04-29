import dbPojo.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;

public class SocketServer extends Thread{
    private final DatagramSocket socket;
    private final DBState dbState;
    private final int port;
    private final SocketClient socketClient;

    private DemonTimer demonTimer;

    private int seed;

    public SocketServer(int port, DBState dbState, int seed) throws SocketException{
        this.port = port;
        this.socket = new DatagramSocket(port);
        this.dbState = dbState;
        this.socketClient = new SocketClient();
        this.demonTimer = new DemonTimer(dbState);
        this.seed = seed;
        new Thread(() -> {
            int count =0;
            while (true) {
                try {
                    Thread.sleep(5000); // Send heartbeat messages every 5 seconds
                    sendHeartbeatMessages();
                    count++;
                    if(count>1){
                        decideClusterStatus();
                        count=0;
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void sendHeartbeatMessages() throws IOException {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("type", "heartbeat_message");
        dataMap.put("message", port);
        for(int otherPort : dbState.ports){
            socketClient.sendData("localhost", otherPort, dataMap);
        }
        if(!Arrays.stream(dbState.ports).anyMatch(x -> x == seed) && seed !=-1){
            socketClient.sendData("localhost", seed, dataMap);
        }
    }

    private void decideClusterStatus() throws IOException, InterruptedException {
        dbState.aliveList = new ArrayList<>();
        dbState.deadList = new ArrayList<>();
        boolean change = false;
        long time = System.currentTimeMillis();
        for(int recport : dbState.heartBeatMap.keySet()){
            if(time-dbState.heartBeatMap.get(recport) <= 10000){
                dbState.aliveList.add(recport);
                if(!Arrays.stream(dbState.ports).anyMatch(x -> x == recport)){
                    change = true;
                }
            }
        }

        for(int recport : dbState.ports){
            if(!dbState.aliveList.contains(recport)){
                dbState.deadList.add(recport);
                change = true;
            }
        }
        dbState.change = change;
        if(change){
            System.out.println("######## identified change in configuration#########");
            System.out.println("current config: " + Arrays.toString(dbState.ports));
            System.out.println("alive list "+dbState.aliveList);
            System.out.println("dead list "+dbState.deadList);
            System.out.println("######## config change#########");
            Random rand = new Random();
            Thread.sleep(rand.nextInt(2)*1000);
            if(dbState.change && (dbState.election_start == null || System.currentTimeMillis()-dbState.election_start > 20000)){
                Map<String, Object> joinMap = new HashMap<>();
                joinMap.put("type", "join_message");
                JoinMessage message = JoinMessage.builder().alive_list(dbState.aliveList).dead_list(dbState.deadList).candidate(port).term(dbState.term+1).build();
                joinMap.put("message", message);
                dbState.election_start = System.currentTimeMillis();
                dbState.votes = new HashSet<>();
                dbState.votes.add(port);
                dbState.term++;
                dbState.candidate = port;
                for(int otherport : dbState.ports){
                    if(otherport !=port)
                        socketClient.sendData("localhost", otherport,joinMap);
                }
                if(!Arrays.stream(dbState.ports).anyMatch(x -> x == seed) && seed !=-1){
                    socketClient.sendData("localhost", seed, joinMap);
                }
            }
        }
    }
    public void run(){
        try {
            byte[] buf = new byte[2560000];
            for(int port: dbState.ports){
                dbState.aliveList.add(port);
            }
            if(port == dbState.ports[0]){
                TokenMessage message = new TokenMessage();
                message.sender_port = port;
                Map<String, Object> map = new HashMap<>();
                map.put("type", "token_message");
                map.put("message", message);
                socketClient.sendData("localhost", dbState.ports[0], map);
            }

            while(true){
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                Map<String, Object> dataMap = parseData(packet);
                if(dataMap.get("type").equals("token_message")) {
                    TokenMessage token = (TokenMessage) dataMap.get("message");
                    Map<String, Object> reply = new HashMap<>();
                    reply.put("type", "ack");
                    reply.put("message", port);
                    if(token.sender_port != port  || dbState.ports.length ==1) {
                        socketClient.sendData("localhost", token.sender_port, reply);
                    }
                    int max = Math.max(dbState.term, token.term);
                    dbState.term = max;
                    token.term = max+1;
                    while (dbState.requestQueue.size() > 0) {
                        RegularMessage message = dbState.requestQueue.pollFirst();
                        token.global_seq++;
                        message.seqNumber = token.global_seq;
                        Map<String, Object> newMap = new HashMap();
                        newMap.put("type", "regular_message");
                        newMap.put("message", message);
                        for (int port : dbState.ports) {
                            socketClient.sendData("localhost", port, newMap);
                        }
                    }
                    dbState.global_seq = token.global_seq;
                    int index = 0;
                    for (int i = 0; i < dbState.ports.length; i++) {
                        if (dbState.ports[i] == port) {
                            index = i;
                            break;
                        }
                    }
                    token.sender_port = port;
                    if (token.min_aru == null) {
                        token.min_aru = dbState.local_aru;
                        token.min_aru_port = port;
                    } else if (token.min_aru > dbState.local_aru || token.min_aru_port == port) {
                        token.min_aru = dbState.local_aru;
                        token.min_aru_port = port;
                    }
                    if (port == dbState.ports[0]) {
                        token.count++;
                    }
                    dataMap.put("message", token);
                    dbState.nextTokenMap = dataMap;
                    dbState.nextPort = dbState.ports[(index + 1) % dbState.ports.length];
                    socketClient.sendData("localhost", dbState.nextPort, dataMap);
                    demonTimer.startTimer(4000, index);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (token.count > 1) {
                        int min = token.min_aru;
                        List<Integer> keySet = new ArrayList();
                        for (int key : dbState.storage.keySet()) {
                            if (key < min) {
                                keySet.add(key);
                            }
                        }

                        for (int key : keySet) {
                            dbState.storage.remove(key);
                        }
                    }
                } else if(dataMap.get("type").equals("regular_message")){
                    RegularMessage message = (RegularMessage) dataMap.get("message");
                    if(!dbState.storage.containsKey(message.seqNumber)) {
                        dbState.receivedQueue.add(message);
                        dbState.storage.put(message.seqNumber, message);
                    }
                } else if(dataMap.get("type").equals("retransmission_message")){
                    ReTransmissionMessage message = (ReTransmissionMessage)  dataMap.get("message");
                    if(dbState.storage.containsKey(message.global_seq)){
                        Map<String, Object> newMap = new HashMap();
                        newMap.put("type", "regular_message");
                        newMap.put("message", dbState.storage.get(message.global_seq));
                        socketClient.sendData("localhost", message.sender_port, newMap);
                    }
                }else if(dataMap.get("type").equals("ack")){
                    demonTimer.resetTimer();
                    System.out.println("receive token ack from the sender "+dataMap.get("message"));
                } else if(dataMap.get("type").equals("heartbeat_message")){
                    int recport = (int) dataMap.get("message");
                    System.out.println("received heartbeat "+recport);
                    dbState.heartBeatMap.put(recport, System.currentTimeMillis());
                } else if(dataMap.get("type").equals("join_message")){
                    JoinMessage message = (JoinMessage) dataMap.get("message");
                    if(message.term < dbState.term){
                        System.out.println("Not voting for the change due to term");
                    }
                    if(message.alive_list.size() >= dbState.aliveList.size() || message.dead_list.size() >= dbState.deadList.size()){
                        dbState.aliveList = message.alive_list;
                        dbState.deadList = message.dead_list;
                        dbState.candidate = message.candidate;
                        Map<String, Object> joinack = new HashMap<>();
                        joinack.put("type","join_ack");
                        joinack.put("message", port);
                        socketClient.sendData("localhost", message.candidate, joinack);
                        System.out.println("######## Sent Ack for the Join #####");
                    }
                } else if(dataMap.get("type").equals("reconfig_message")){
                    System.out.println("######### reconfigured cluster #########");
                    ReConfigurationMessage message = (ReConfigurationMessage) dataMap.get("message");
                    dbState.ports = message.ports;
                    System.out.println(Arrays.toString(dbState.ports));
                } else if(dataMap.get("type").equals("join_ack")){
                    int sender = (int) dataMap.get("message");
                    if(dbState.candidate == port){
                        dbState.votes.add(sender);
                        System.out.print("received acks from "+dbState.votes);
                        if(dbState.votes.size() > dbState.votes.size()/2){
                            System.out.println("####reached consensus on cluster######");
                            dbState.ports = new int[dbState.aliveList.size()];
                            for(int i=0;i<dbState.aliveList.size();i++){
                                dbState.ports[i] = dbState.aliveList.get(i);
                            }
                            Map<String, Object> reconfigmap = new HashMap<>();
                            reconfigmap.put("type", "reconfig_message");
                            ReConfigurationMessage message = ReConfigurationMessage.builder().ports(dbState.ports).build();
                            reconfigmap.put("message", message);
                            dbState.term++;
                            for(int otherport : dbState.ports){
                                socketClient.sendData("localhost", otherport, reconfigmap);
                            }
                        }
                    }
                }
                System.out.println("machines in consensus "+ Arrays.toString(dbState.ports));
                //System.out.println("dbState "+dbState.requestQueue.size()+" "+dbState.receivedQueue.size()+" "+dbState.storage.size()+" "+dbState.local_aru+" "+dbState.global_seq);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> parseData(DatagramPacket packet) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Map<String, Object> dataMap = (Map<String, Object>) ois.readObject();
        return dataMap;
    }
}
