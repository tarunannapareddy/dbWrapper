import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class GRPCServer extends Thread{

    private final DBState dbState;
    private final int port;

    private final int socket_port;

    public GRPCServer(DBState dbState, int port, int socket_port) {
        this.dbState = dbState;
        this.port = port;
        this.socket_port = socket_port;
    }

    public void run(){
        Server server = ServerBuilder
                .forPort(port)
                .addService(new Execute(dbState,socket_port ))
                .build();

        try {
            server.start();
            server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
