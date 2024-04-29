import dbPojo.DBState;
import dbPojo.RegularMessage;
import dbservice.DBServiceGrpc;

public class Execute extends DBServiceGrpc.DBServiceImplBase {

    private final DBState dbState;
    private final int port;

    public Execute(DBState dbState, int port){
        this.dbState = dbState;
        this.port = port;
    }

    public void execute(dbservice.Execute.QueryRequest request,
                        io.grpc.stub.StreamObserver<dbservice.Execute.ExecuteResponse> responseObserver) {
        RegularMessage regularMessage = new RegularMessage( null, dbState.local_seq, request.getTable(), request.getFunction(), request.getInput(), port);
        int state = dbState.local_seq;
        dbState.queryResponse.put(dbState.local_seq, null);
        dbState.local_seq++;
        dbState.requestQueue.addLast(regularMessage);
        String response = null;
        while(response == null){
            response = dbState.queryResponse.get(state);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("responding to user"+ response);
        responseObserver.onNext(dbservice.Execute.ExecuteResponse.newBuilder().setResponse(response).build());
        responseObserver.onCompleted();
    }
}
