import dbPojo.DBState;

import java.io.IOException;
import java.net.SocketException;


public class main3 {

    public static void start_service(int[] servicePorts, int[] ports, int seed) throws SocketException {
        DBState dbState = new DBState();
        dbState.ports = ports;
        GRPCServer grpcServer = new GRPCServer(dbState, servicePorts[0],servicePorts[1]);
        grpcServer.start();
        SocketServer socketServer = new SocketServer(servicePorts[1], dbState, seed);
        socketServer.start();
        MessageProcessor messageProcessor = new MessageProcessor(dbState,servicePorts[1]);
        messageProcessor.start();
    }

    public static void main(String[] args) throws IOException {
        int[] service3 = new int []{4020,4003};
        int[] ports = new int[]{4003};
        start_service(service3, ports,4002);
    }

}

