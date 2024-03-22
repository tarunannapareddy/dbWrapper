import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocketServer extends Thread{
    private final DatagramSocket socket;
    private final DBState dbState;
    private final int port;
    private final SocketClient socketClient;

    private DemonTimer demonTimer;

    public SocketServer(int port, DBState dbState) throws SocketException{
        this.port = port;
        this.socket = new DatagramSocket(port);
        this.dbState = dbState;
        this.socketClient = new SocketClient();
        this.demonTimer = new DemonTimer(dbState);
    }

    public void run(){
        try {
            byte[] buf = new byte[2560000];

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
                    if(token.sender_port != port) {
                        socketClient.sendData("localhost", token.sender_port, reply);
                    }
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
                    demonTimer.startTimer(5000);
                    try {
                        Thread.sleep(5000);
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
                }else if(dataMap.get("type").equals("retransmission_message")){
                    ReTransmissionMessage message = (ReTransmissionMessage)  dataMap.get("message");
                    if(dbState.storage.containsKey(message.global_seq)){
                        Map<String, Object> newMap = new HashMap();
                        newMap.put("type", "regular_message");
                        newMap.put("message", dbState.storage.get(message.global_seq));
                        socketClient.sendData("localhost", message.sender_port, newMap);
                    }
                }else if(dataMap.get("type").equals("ack")){
                    demonTimer.resetTimer();
                }
                System.out.println("dbState "+dbState.requestQueue.size()+" "+dbState.receivedQueue.size()+" "+dbState.storage.size()+" "+dbState.local_aru+" "+dbState.global_seq);
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
