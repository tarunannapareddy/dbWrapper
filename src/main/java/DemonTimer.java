import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class DemonTimer {
    private Timer timer;
    private final DBState dbState;
    private final SocketClient socketClient;


    public DemonTimer(DBState dbState ) throws SocketException {
        this.timer = new Timer(true); // Daemon thread
        this.dbState = dbState;
        this.socketClient = new SocketClient();
    }

    public void startTimer(long delay) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    System.out.println("Timer expired!");
                    socketClient.sendData("localhost", dbState.nextPort, dbState.nextTokenMap);
                    startTimer(5000);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        timer.schedule(task, delay);
    }

    public void resetTimer() {
        timer.cancel();
        this.timer = new Timer(true); // Daemon thread
    }
}
