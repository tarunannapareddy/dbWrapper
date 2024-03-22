import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MessageProcessor extends Thread{
    private final DBState dbState;
    private final SocketClient socketClient;
    private final int port;

    public MessageProcessor(DBState dbState, int port) throws SocketException {
        this.dbState = dbState;
        this.socketClient = new SocketClient();
        this.port = port;
    }

    public void run(){
        while(true){
            RegularMessage message = dbState.receivedQueue.peek();
            if(message == null){
                if(dbState.local_aru< dbState.global_seq){
                    sendMessage();
                }
            } else {
                if (dbState.local_aru.equals(message.seqNumber - 1)) {
                    message = dbState.receivedQueue.poll();
                    System.out.println("executing the request" + message.seqNumber+ message.message.toString());
                    if(message.source == port){
                        dbState.queryResponse.put(message.localSeqNumber, "success");
                    }
                    dbState.local_aru++;
                } else {
                    sendMessage();

                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendMessage(){
        Map<String, Object> map = new HashMap();
        map.put("type", "retransmission_message");
        ReTransmissionMessage reTransmissionMessage = new ReTransmissionMessage(port, dbState.local_aru + 1);
        map.put("message", reTransmissionMessage);
        System.out.println("sent retransmission message to " +port +reTransmissionMessage);
        try {
            Random rand = new Random();
            int reqPort =  dbState.ports[rand.nextInt(dbState.ports.length)];
            while(reqPort ==port){
                reqPort =  dbState.ports[rand.nextInt(dbState.ports.length)];
            }
            socketClient.sendData("localhost",reqPort, map);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
