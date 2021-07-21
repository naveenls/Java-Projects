import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

public class SenderSR {
    static int MAX_PACKET_LENGTH, PACKET_GEN_RATE, WINDOW_SIZE, MAX_PACKETS, BUFFER_SIZE, recevierPort, seqBits;
    static String receiverIP;
    static boolean debugMode = false;
    static DatagramSocket ds;
    static InetAddress recvAddr;
    static Timer globalTimer;

    static long rttSum = 0, count = 0,totalTransmission = 0;

    static long getRTT(){
        synchronized (SenderSR.class) {
            if (count <= 10) return 150;
            return (rttSum) / count;
        }
    }
    static void addRTT(long rtt){
        synchronized (SenderSR.class){
            rttSum += rtt;
            count++;
        }
    }
    static class Packet
    {
        int seqNum, state;
        int numSent;

        DatagramPacket pkt;
        long sentTime, genTime;

        public Packet(int seqNum, byte[] data) {
            this.seqNum = seqNum;
            state = 0; numSent = 0;
            pkt = new DatagramPacket(data, data.length, recvAddr, recevierPort);
            genTime = System.nanoTime();
        }
        public void startTimer(){
            numSent++; totalTransmission++;
            globalTimer.schedule(new packetTimer(this), 2*getRTT());
        }
    }
    static class Buffer
    {
        Packet[] buf;
        Hashtable<Integer, Packet> seqMap;

        int front, rear, nextUsable;

        public Buffer() {
            buf = new Packet[BUFFER_SIZE];
            seqMap = new Hashtable<>();
            front = -1;
            nextUsable = 0;
            rear = 0;
        }

        boolean isFull(){
            return ((front == 0 && rear == BUFFER_SIZE-1) || front == rear+1);
        }
        boolean isEmpty(){
            return (front == -1);
        }
        void insertRear(Packet pkt)
        {
            if(front == -1){
                front = 0;
                rear = 0;
                nextUsable = 0;
            }
            else rear = (rear+1)%BUFFER_SIZE;
            buf[rear] = pkt;
        }
        boolean deleteFront()
        {
            if(isEmpty()) return false;
            buf[front] = null;

            if(front == rear) front = -1;
            else front = (front+1)%BUFFER_SIZE;
            return true;
        }
        Packet getFront(){
            if(isEmpty()) return null;
            return buf[front];
        }
        Packet getPacket(int seqNum){
            return seqMap.get(seqNum);
        }
        Packet getNextPacket(){
            if(isEmpty() || (nextUsable == (front + WINDOW_SIZE)%BUFFER_SIZE)) return null;
            Packet pkt = buf[nextUsable];

            if(pkt != null){
                seqMap.put(pkt.seqNum, pkt);
                nextUsable = (nextUsable+1)%BUFFER_SIZE;
            }
            return pkt;
        }
    }
    static int getInt(byte[] arr, int i){ //Returns 4 byte Integer from given array
        return ((arr[i] & 0xFF) << 24) |
                ((arr[i+1] & 0xFF) << 16) |
                ((arr[i+2] & 0xFF) << 8 ) |
                ((arr[i+3] & 0xFF));
    }
    static void addInt(byte[] arr, int i, int val){ //Stores 4 byte Integer to given array
        arr[i] = (byte) (val >> 24);
        arr[i+1] = (byte) (val >> 16);
        arr[i+2] = (byte) (val >> 8);
        arr[i+3] = (byte) (val);
    }
    static class genPacket extends Thread{
        final Buffer b;
        int currSeqNum;
        final int N;
        private Random lenGen;

        int packetLength(){
            return lenGen.nextInt(MAX_PACKET_LENGTH-39)+40;
        }
        genPacket(Buffer b) {
            this.b = b;
            this.N = (1<<seqBits);
            currSeqNum = 0;
            lenGen = new Random();
        }

        @Override
        public void run() {
            while (true){
                synchronized (b){
                    if(!b.isFull()){
                        int pktLength = packetLength();
                        byte[] data = new byte[pktLength + 8];
                        addInt(data,0,pktLength);
                        addInt(data,4,currSeqNum);

                        Packet pkt = new Packet(currSeqNum,data);
                        b.insertRear(pkt); b.notify();
                        currSeqNum = (currSeqNum+1)%N;
                    }
                }
                try {
                    int sleepTime = (int) Math.ceil(1000.0/(double) PACKET_GEN_RATE);
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    static class sendPacket extends Thread{
        final Buffer b;
        sendPacket(Buffer b) {
            this.b = b;
        }
        @Override
        public void run() {
            try {
                while (true) {
                    synchronized (b){
                        Packet pkt = b.getFront();
                        while (pkt != null && pkt.state == 2){
                            b.deleteFront();
                            pkt = b.getFront();
                        }

                        pkt = b.getNextPacket();
                        while (pkt != null){
                            ds.send(pkt.pkt);
                            pkt.state = 1; pkt.startTimer();
                            pkt.sentTime = System.currentTimeMillis();
                            pkt = b.getNextPacket();
                        }
                        b.wait();
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }

        }
    }
    static class receiveACK extends Thread{
        final Buffer b;
        receiveACK(Buffer b) {
            this.b = b;
        }

        @Override
        public void run() {
            DatagramPacket ackPaket;
            byte[] seqNum = new byte[4];

            try {
                while (true){
                    ackPaket = new DatagramPacket(seqNum, seqNum.length);
                    ds.receive(ackPaket);

                    synchronized (b) {
                        int seqNumber = getInt(seqNum, 0);
                        Packet pkt = b.getPacket(seqNumber);
                        synchronized (pkt) {
                            if(pkt.state == 2) continue;

                            pkt.state = 2;
                            long rttCurr = System.currentTimeMillis() - pkt.sentTime;
                            long microTime = pkt.genTime/1000;
                            if(debugMode) System.out.printf("Seq %d: Time Generated: %d:%d RTT: %d Number of Attempts: %d\n",seqNumber, microTime/1000, microTime%1000, rttCurr, pkt.numSent);
                            addRTT(rttCurr);
                            if(count >= MAX_PACKETS) {
                                globalTimer.cancel(); ds.close();
                                System.out.println("PACKET_GEN_RATE: " + PACKET_GEN_RATE);
                                System.out.println("PACKET_LENGTH: " + MAX_PACKET_LENGTH);
                                System.out.println("Re-Transmission Ratio: " + (double)totalTransmission/count);
                                System.out.println("Average RTT: " + getRTT());
                                System.exit(0);
                            }
                        }
                        b.notify();
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }

        }
    }
    static class packetTimer extends TimerTask{
        final Packet pkt;
        packetTimer(Packet pkt) {
            this.pkt = pkt;
        }
        @Override
        public void run() {
            try {
                synchronized (pkt){
                    if(pkt.state == 1){
                        ds.send(pkt.pkt);
                        if(pkt.numSent >= 10) {
                            globalTimer.cancel(); ds.close();
                            System.out.println("PACKET_GEN_RATE: " + PACKET_GEN_RATE);
                            System.out.println("PACKET_LENGTH: " + MAX_PACKET_LENGTH);
                            System.out.println("Re-Transmission Ratio: " + (double)totalTransmission/count);
                            System.out.println("Average RTT: " + getRTT());
                            System.exit(0);
                        }
                        pkt.startTimer();
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public static void initializeParams(String[] args) {
        for(int i=0;i< args.length;i+=2)
        {
            switch (args[i]){
                case "-d": {
                    debugMode = true; i--;
                    break;
                }
                case "-s": {
                    receiverIP = args[i+1];
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
                case "-L": {
                    MAX_PACKET_LENGTH = Integer.parseInt(args[i+1]);
                    break;
                }
                case "-R": {
                    PACKET_GEN_RATE = Integer.parseInt(args[i+1]);
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
            }
        }

        try {
            ds = new DatagramSocket();
            globalTimer = new Timer();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        try{
            String IPV4_REGEX =  "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$";
            Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

            if (IPv4_PATTERN.matcher(receiverIP).matches()) {
                String[] addr = receiverIP.split("\\.");
                byte[] byteAddr = new byte[4];

                for(int i=0;i<4;i++){
                    byteAddr[i] = (byte) Integer.parseInt(addr[i]);
                }
                recvAddr = InetAddress.getByAddress(byteAddr);
            } else {
                recvAddr = InetAddress.getByName(receiverIP);
            }
        } catch (UnknownHostException e){
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        initializeParams(args);
        Buffer buf = new Buffer();

        try {
              Thread.sleep(1000);
              //System.out.println("SR Sender started");
              (new genPacket(buf)).start();
              (new sendPacket(buf)).start();
              (new receiveACK(buf)).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
