import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.Map;

public class SocketClient {
    private DatagramSocket socket;

    public SocketClient() throws SocketException {
        socket =new DatagramSocket();
    }

    public void sendData(String ip, int port, Map<String, Object> msg) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(msg);
        oos.flush();
        byte[] data = baos.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(ip), port);
        socket.send(packet);
    }
}
