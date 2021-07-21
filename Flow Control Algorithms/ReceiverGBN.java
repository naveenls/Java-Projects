import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Random;

public class ReceiverGBN {
    static int recevierPort, seqBits, MAX_PACKETS;
    static double PACKET_ERROR_RATE;
    static boolean debugMode = false;

    static DatagramSocket ds;
    static Random r = new Random();

    public static void initializeParams(String[] args) {
        for(int i=0;i< args.length;i+=2)
        {
            switch (args[i]){
                case "-d": {
                    debugMode = true; i--;
                    break;
                }
                case "-p": {
                    recevierPort = Integer.parseInt(args[i+1]);
                    break;
                }
                case "-n": {
                    seqBits = Integer.parseInt(args[i+1]);
                    break;
                }
                case "-N": {
                    MAX_PACKETS = Integer.parseInt(args[i+1]);
                    break;
                }
                case "-e": {
                    PACKET_ERROR_RATE = Double.parseDouble(args[i+1]);
                    break;
                }
            }
        }
        try {
            ds = new DatagramSocket(recevierPort);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    static boolean isDropped()
    {
        int result = r.nextInt(10000);
        return (result < PACKET_ERROR_RATE * 10000.0);
    }
    static void addInt(byte[] arr, int i, int val){ //Stores 4 byte Integer to given array
        arr[i] = (byte) (val >> 24);
        arr[i+1] = (byte) (val >> 16);
        arr[i+2] = (byte) (val >> 8);
        arr[i+3] = (byte) (val);
    }
    static int getInt(byte[] arr, int i){ //Returns 4 byte Integer from given array
        return ((arr[i] & 0xFF) << 24) |
                ((arr[i+1] & 0xFF) << 16) |
                ((arr[i+2] & 0xFF) << 8 ) |
                ((arr[i+3] & 0xFF));
    }
    public static void main(String[] args) throws IOException {
        initializeParams(args);
        DatagramPacket dp;
        byte[] pkt = new byte[1024];
        int N = (1<<seqBits), expectedSeq = 0, count = 0;

        while (count < MAX_PACKETS){
            dp = new DatagramPacket(pkt,pkt.length);
            ds.receive(dp);

            int length = getInt(pkt,0);
            int seqNum = getInt(pkt,4);
            byte[] data = new byte[length];
            System.arraycopy(pkt, 8, data, 0, length);

            if(!isDropped()){
                byte[] seqPkt = new byte[4];
                if(expectedSeq%N == seqNum){
                    addInt(seqPkt,0, seqNum);
                    long microTime = System.nanoTime()/1000;
                    if(debugMode) System.out.printf("Seq %d: Time Received: %d:%d\n", seqNum, microTime/1000, microTime%1000);
                    expectedSeq++; count++;
                }
                else {
                    addInt(seqPkt,0, (expectedSeq-1+N)%N);
                }
                ds.send(new DatagramPacket(seqPkt, seqPkt.length, dp.getAddress(), dp.getPort()));
            }
        }
    }
}
