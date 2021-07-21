//package com.company;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class Pair<K extends Comparable<K>, V> implements Comparable<Pair<K, V>>{ //To store a pair of values
    K key;
    V value;

    Pair(K k, V v){
        key = k;
        value = v;
    }
    public K getKey(){
        return key;
    }
    public V getValue(){
        return value;
    }

    @Override
    public int compareTo(Pair<K, V> o) {
        return key.compareTo(o.getKey());
    }
}

class Graph{
    int numObjects;
    private ArrayList<Hashtable<Integer,Integer> > adj; //Adjacency list
    private Hashtable<Integer, Pair<Integer,Integer> > wRange; //Edge weights range
    int[] seqNum, par;
    int[] dist;
    static final int maxV = Integer.MAX_VALUE;

    Graph(int n){
        numObjects = n;
        adj = new ArrayList<>();
        wRange = new Hashtable<>();
        seqNum = new int[n];
        par = new int[n];
        dist = new int[n];

        for(int i=0;i<n;i++){
            adj.add(new Hashtable<>());
            seqNum[i] = -1;
        }
    }

    public Iterator<Integer> getAdj(int i){
        return wRange.keySet().iterator();
    }
    public synchronized int getEdge(int u,int v){
        return adj.get(u).get(v);
    }
    public int getAdjLength(int i){
        return wRange.size();
    }
    public synchronized void addEdge(int u,int v,int w){
        adj.get(u).put(v,w);
        adj.get(v).put(u,w);
    }
    public void addRange(int v, int l,int r){
        wRange.put(v, new Pair<>(l,r));
    }
    public Pair<Integer, Integer> getRange(int v){
        return wRange.get(v);
    }
    public synchronized void shortestPath(int src){ //Dijkstra's algorithm
        for(int i=0;i<numObjects;i++) {
            dist[i] = maxV;
            par[i] = -1;
        }
        PriorityQueue<Pair<Integer, Integer> > pq = new PriorityQueue<>();
        pq.add(new Pair<>(0, src));
        dist[src] = 0;

        while (!pq.isEmpty())
        {
            Pair<Integer,Integer> pr = pq.remove();
            int u = pr.getValue();
            if(dist[u] < pr.getKey()) continue;

            Iterator<Integer> itr = adj.get(u).keySet().iterator();
            while (itr.hasNext()){
                int dest = itr.next();
                int w = adj.get(u).get(dest);
                if(w != maxV && dist[dest] > dist[u] + w){
                    dist[dest] = dist[u] + w; par[dest] = u;
                    pq.add(new Pair<>(dist[dest], dest));
                }
            }
        }
    }
}

public class ospf {
    static int id;
    static String inFile, outFile;
    static int hi, lsai, spfi;
    static int base = 10000;

    static Random random = new Random();
    static AtomicInteger cnt = new AtomicInteger(0);

    static FileWriter outObj;
    static Graph g;

    static DatagramSocket ds;

    static void initializeParams(String[] args){
        hi = 1;
        lsai = 5;
        spfi = 20;
        for(int i=0;i<args.length;i+=2){
            switch (args[i])
            {
                case "-i":
                    id = Integer.parseInt(args[i+1]);
                    break;
                case "-f":
                    inFile = args[i+1] + ".txt";
                    break;
                case "-o":
                    outFile = args[i+1] + ".txt";
                    break;
                case "-h":
                    hi = Integer.parseInt(args[i+1]);
                    break;
                case "-a":
                    lsai = Integer.parseInt(args[i+1]);
                    break;
                case "-s":
                    spfi = Integer.parseInt(args[i+1]);
                    break;
            }
        }
    }

    static int getInt(byte[] arr, int i){ //Returs 4 byte Integer from given array
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

    static void sendTo(byte[] arr, int toId) { //Sends the given packet to given receiver
        try {
            InetAddress ip = InetAddress.getLocalHost();
            DatagramPacket DpSend = new DatagramPacket(arr, arr.length, ip, base + toId);
            ds.send(DpSend);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class helloSend extends Thread{ //Sends hello message once every hi seconds
        @Override
        public void run() {
            try {
                while (true) {
                    while (!cnt.compareAndSet(0,0)); //Proceeds only when all hello replies are received

                    byte[] arr = new byte[1024];
                    String type = "HELLO";
                    for(int i=0;i<type.length();i++)
                        arr[i] = (byte) type.charAt(i);
                    addInt(arr, 5, id);

                    Iterator<Integer> adjList = g.getAdj(id);
                    while (adjList.hasNext()){
                        int dest = adjList.next();
                        if(dest > id){
                            sendTo(arr, dest);
                            //System.out.println("HELLO sent to " + dest + "\n");
                            cnt.incrementAndGet();
                        }
                    }
                    Thread.sleep(hi * 1000);
                }
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
    static class lsaSend extends Thread{ //Generates and sends LSA message once every lsai seconds
        @Override
        public void run() {
            try {
                while (true) {
                    byte[] arr = new byte[1024];
                    String type = "LSA";
                    for (int i = 0; i < type.length(); i++)
                        arr[i] = (byte) type.charAt(i);

                    int start = 3;
                    addInt(arr, start, id); start += 4;
                    addInt(arr, start, ++g.seqNum[id]); start += 4;

                    addInt(arr, start, g.getAdjLength(id)); start += 4;

                    Iterator<Integer> itr = g.getAdj(id);
                    while (itr.hasNext()) {
                        int dest = itr.next();
                        addInt(arr, start, dest); start += 4;
                        addInt(arr, start, g.getEdge(id, dest)); start += 4;
                        //System.out.println("LSA sent to " + dest + "\n");
                    }
                    itr = g.getAdj(id);
                    while (itr.hasNext()) {
                        int dest = itr.next();
                        sendTo(arr, dest);
                    }
                    Thread.sleep(lsai * 1000);
                }
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
    static class calcSPF extends Thread{ //Computes and stores routing table once every spfi seconds
        @Override
        public void run() {
            int numItr = 1;
            try {
                Thread.sleep(spfi * 1000);
                while (true){
                    g.shortestPath(id);
                    //System.out.println("Claculating SPF\n");
                    outObj = new FileWriter(outFile, true);
                    outObj.write("Routing Table for Node No. "+ (id+1) + " at Time " + numItr*spfi + "\n");
                    outObj.write("Destination Path Cost\n");

                    for(int i=0;i<g.numObjects;i++)
                    {
                        if(i == id) continue;
                        StringBuilder row = new StringBuilder();
                        row.append((i+1) + " ");

                        if(g.dist[i] == Integer.MAX_VALUE){
                            row.append("No Path ");
                        }
                        else {
                            Stack<Integer> s = new Stack<>();
                            int currPar = g.par[i];
                            while (currPar != -1) {
                                s.add(currPar);
                                currPar = g.par[currPar];
                            }
			     
                            while (!s.isEmpty()) {
                                row.append((s.pop()+1)+",");
                            }
                            row.append((i+1)+" ");
                            row.append(g.dist[i]);
                        }
                        outObj.write(row.toString() + "\n");
                    }
                    numItr++; outObj.close();
                    Thread.sleep(spfi * 1000);
                }
            } catch (InterruptedException | IOException e){
                e.printStackTrace();
            }
        }
    }
    static class receiveForward extends Thread{ //Receives all the packets sent to port (10000+id)
        @Override
        public void run()
        {
            try {
                DatagramPacket DpReceive;
                byte[] rcvData = new byte[1024];

                while (true){
                    DpReceive = new DatagramPacket(rcvData, rcvData.length);
                    ds.receive(DpReceive);

                    String msg = new String(rcvData);
                    if(msg.startsWith("LSA"))
                    {
                        int start = 3;
                        int src = getInt(rcvData, start); start += 4;
                        int lsaSeqNum = getInt(rcvData,start); start += 4;
                        //System.out.println("LSA received from " + src + "\n");

                        if(g.seqNum[src] < lsaSeqNum) { //Checking validity of LSA message
                            g.seqNum[src] = lsaSeqNum;

                            int numEntries = getInt(rcvData, start); start += 4;
                            int dest, weight;
                            while (numEntries-- > 0) { //Updating the graph
                                dest = getInt(rcvData, start); start += 4;
                                weight = getInt(rcvData, start); start += 4;

                                if(dest != id) g.addEdge(src, dest, weight); //Neighbour information is up-to-date
                            }

                            Iterator<Integer> itr = g.getAdj(id);
                            while (itr.hasNext()){ //Forwarding LSA message to neighbours
                                dest = itr.next();
                                if(DpReceive.getPort() != base+dest){ sendTo(rcvData, dest); } //System.out.println("LSA forwarded to " + dest + "\n");
                            }
                        }
                    }
                    else if(msg.startsWith("HELLOREPLY"))
                    {
                        int src = getInt(rcvData, 10);
                        int nWeight = getInt(rcvData, 18);
                        g.addEdge(id, src, nWeight); //Updating the link cost

                        cnt.decrementAndGet();
                        //System.out.println("HELLOREPLY received from " + src + "\n");
                    }
                    else if(msg.startsWith("HELLO"))
                    {
                        int src = getInt(rcvData, 5);
                        byte[] output = new byte[1024];
                        //System.out.println("HELLO received from " + src + "\n");

                        String type = "HELLOREPLY";
                        for(int i=0;i<type.length();i++){
                            output[i] = (byte) type.charAt(i);
                        }
                        addInt(output, 10, id);
                        addInt(output, 14, src);

                        Pair<Integer,Integer> range = g.getRange(src);
                        int nWeight = random.nextInt(range.getValue() - range.getKey() + 1) + range.getKey(); //Generating a new random Integer within given range
                        g.addEdge(id, src, nWeight);

                        addInt(output, 18, nWeight);
                        sendTo(output, src); //Sending HELLOREPLY to the source
                    }
                    rcvData = new byte[1024];
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) {
        // write your code here
        initializeParams(args);
        try {
            outObj = new FileWriter(outFile); outObj.close();
            File inObj = new File(inFile);
            Scanner input = new Scanner(inObj);
            ds = new DatagramSocket(base+id, InetAddress.getLocalHost());

            int n = input.nextInt();
            int m = input.nextInt();
            g = new Graph(n);

            int u,v,l,r;
            for(int i=0;i<m;i++){
                u = input.nextInt();
                v = input.nextInt();
                l = input.nextInt();
                r = input.nextInt();

                if(u == id) { g.addRange(v, l, r); g.addEdge(u,v,Integer.MAX_VALUE); }
                else if(v == id) { g.addRange(u, l, r); g.addEdge(u,v,Integer.MAX_VALUE); }
            }

            Thread.sleep(1000);

            new receiveForward().start();
            new helloSend().start();
            new lsaSend().start();
            new calcSPF().start();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
