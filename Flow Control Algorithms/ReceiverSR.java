import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Random;

public class ReceiverSR {
    static int WINDOW_SIZE, MAX_PACKETS, BUFFER_SIZE, recevierPort, seqBits;
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
                case "-W": {
                    WINDOW_SIZE = Integer.parseInt(args[i+1]);
                    break;
                }
                case "-B": {
                    BUFFER_SIZE = Integer.parseInt(args[i+1]);
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
    static class Buffer
    {
        byte[][] buf;
        long[] timeReceived;

        int base, N;

        public Buffer() {
            buf = new byte[BUFFER_SIZE][];
            timeReceived = new long[BUFFER_SIZE];
            N = (1<<seqBits);
            base = 0;
        }
        boolean isFull(){
            return base == BUFFER_SIZE;
        }
        private boolean inBetween(int l,int r, int val){
            l %= N;
            r %= N;

            if(l <= r)
                return (val>=l) && (val<=r);
            else
                return (val<=r) || (val>=l);
        }
        private int getIndex(int l,int seqNum){
            int tempL = l%N;
            if(tempL > seqNum) return (seqNum + N - tempL)+l;
            return (seqNum - tempL)+l;
        }
        boolean sendAck(int seqNum, byte[] data){
            if(isFull()) return false;
            if(base > 0) {
                int l = Math.max(0,base-WINDOW_SIZE);
                if (inBetween(l,base - 1, seqNum)) return true;
            }
            int r = Math.min(BUFFER_SIZE-1,base+WINDOW_SIZE-1);
            if(inBetween(base,r,seqNum)){
                int ind = getIndex(base, seqNum);
                if(buf[ind] == null){
                    buf[ind] = data;
                    timeReceived[ind] = System.nanoTime();
                }
                return true;
            }
            return false;
        }
        void commitPackets(){
            while (base!=BUFFER_SIZE && buf[base] != null) {
                long microTime = timeReceived[base];
                if(debugMode) System.out.printf("Seq %d: Time Received: %d:%d\n",base%N, microTime/1000, microTime%1000);
                base++;
            }
        }
    }
    static boolean isDropped()
    {
        int result = r.nextInt(10000);
        return (result < PACKET_ERROR_RATE * 10000.0);
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
        Buffer b = new Buffer();
        byte[] pkt = new byte[100024];
        int numAck = 0;

        while (numAck < MAX_PACKETS){
            dp = new DatagramPacket(pkt,pkt.length);
            ds.receive(dp);

            int length = getInt(pkt,0);
            int seqNum = getInt(pkt,4);
            byte[] data = new byte[length];
            if (length >= 0) System.arraycopy(pkt, 8, data, 0, length);

            if(!isDropped() && b.sendAck(seqNum, data)){
                numAck++;
                byte[] seqPkt = new byte[4];
                System.arraycopy(pkt, 4, seqPkt, 0, 4);

                ds.send(new DatagramPacket(seqPkt, seqPkt.length, dp.getAddress(), dp.getPort()));
            }
            b.commitPackets();
        }
    }
}
