package mhpUtil;
import java.util.*;
import java.awt.*;

public class StatementUtil
{
    public static int stmtCount = 0;
    public static ArrayList<StatementUtil> stmtList = new ArrayList<>();

    public String obj;
    public String name;
    public String thread;
    public int stmtNum;

    ArrayList<StatementUtil> next;
    public ArrayList<StatementUtil> prev;
    public ArrayList<String> monitors;
    public BitSet gen, kill, out, mhp;

    public HashSet<StatementUtil> notifyPred;
    public BitSet notifySucc;
    public boolean inQueue;

    public StatementUtil(String obj, String name, String thread){
      this.obj = obj;
      this.name = name;
      this.thread = thread;

      next = new ArrayList<>();
      prev = new ArrayList<>();
      monitors = new ArrayList<>();

      for(String iden: staticObs.monitors){
          this.monitors.add(iden);
      }
      inQueue = false;
    }
    StatementUtil() {}
    public StatementUtil getNotifyEntry()
    {
        for(StatementUtil ch: next){
            if(ch.name == "notified_entry") return ch;
        }
        return null;
    }
    public void initSets()
    {
      //System.out.println(obj + " " + name + " " + thread);
      out = new BitSet(stmtCount);
      mhp = new BitSet(stmtCount);
      setGen();
      setKill();

      if(name == "notified_entry"){
          notifyPred = new HashSet<>();
      } else if(name == "notify" || name == "notifyAll"){
          notifySucc = new BitSet(stmtCount);
      }
    }
    public void setGen()
    {
        gen = new BitSet(stmtCount);
        if(name == "start"){
            for(StatementUtil nxt: this.next){
                if(nxt.name == "begin") gen.set(nxt.stmtNum);
            }
        }
        //System.out.println("Gen: " + gen);
    }
    public void setKill()
    {
        switch(name)
        {
            case "join":
            {
                kill = staticObs.sMap.N(obj);
                kill = (BitSet)kill.clone();
                break;
            }
            case "notified_entry":
            case "entry":
            {
                kill = staticObs.sMap.Monitor(obj);
                kill = (BitSet)kill.clone();
                break;
            }
            case "notifyAll":
            {
                kill = staticObs.sMap.waitingNodes(obj);
                kill = (BitSet)kill.clone();
                break;
            }
            case "notify":
            {
                kill = staticObs.sMap.waitingNodes(obj);

                if(kill.cardinality() == 1) kill = (BitSet)kill.clone();
                else kill = new BitSet(stmtCount);
                break;
            }
            default:{
                kill = new BitSet(stmtCount);
            }
        }
        //System.out.println("Kill: " + kill);
    }
    BitSet genNotifyAll()
    {
        BitSet waitingN = staticObs.sMap.waitingNodes(obj);
        StatementUtil waitingPar = prev.get(0);
        BitSet waitingP = waitingPar.mhp;

        BitSet mhpWaiting = (BitSet)waitingP.clone();
        mhpWaiting.and(waitingN);

        BitSet genNotifyAll = new BitSet(StatementUtil.stmtCount);
        int start = mhpWaiting.nextSetBit(0);
        while(start != -1)
        {
            StatementUtil nWaiting = StatementUtil.stmtList.get(start);
            StatementUtil nEntry = nWaiting.getNotifyEntry();

            for(StatementUtil s: notifyPred)  {
                if(s.name == "notifyAll" && nEntry.notifyPred.contains(s))  { genNotifyAll.set(nEntry.stmtNum); break; }
            }
            start = mhpWaiting.nextSetBit(start+1);
        }
        return genNotifyAll;
    }
    public void computeMhp(){
        if(name == "begin")
        {
            for(StatementUtil par: prev) mhp.or(par.out);
            mhp.or(staticObs.sMap.N(thread));
            mhp.xor(staticObs.sMap.N(thread));
        }
        else if(name == "notified_entry")
        {
            BitSet bTemp = new BitSet(stmtCount);
            for(StatementUtil par: notifyPred)  bTemp.or(par.out);
            bTemp.and(prev.get(0).out);
            bTemp.or(genNotifyAll());

            mhp.or(bTemp);
        }
        else{
            for(StatementUtil par: prev)  mhp.or(par.out);
        }
    }
    public boolean computeOut(){
        BitSet outT = (BitSet)out.clone();
        out.clear();

        out.or(mhp);
        out.or(gen);
        out.or(kill); out.xor(kill);

        return (!outT.equals(out));
    }
    public void addNext(StatementUtil s){
        next.add(s);
        s.addPrev(this);
    }
    public void addPrev(StatementUtil s){
        prev.add(s);
    }
    public void addNotifyPred(StatementUtil s){
        notifyPred.add(s);
    }
    public StatementUtil getCopy(String tName){
        StatementUtil cpy = new StatementUtil();
        cpy.obj = this.obj;
        cpy.name = this.name;
        cpy.thread = tName;

        cpy.next = new ArrayList<>();
        cpy.prev = new ArrayList<>();
        cpy.monitors = this.monitors;
        cpy.stmtNum = stmtCount++;
        stmtList.add(cpy);

        return cpy;
    }
    public ArrayList<StatementUtil> getNext(){
        return this.next;
    }
}
