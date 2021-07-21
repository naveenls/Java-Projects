package mhpUtil;
import java.util.*;
import java.awt.*;

public class statementMap
{
    HashMap<String, BitSet> notify;
    HashMap<String, BitSet> notifyAll;
    HashMap<String, BitSet> notify_entry;

    HashMap<String, BitSet> threadNodes;
    HashMap<String, BitSet> waiting;
    HashMap<String, BitSet> monitor;
    HashMap<String, ArrayList<StatementUtil> > labelMap;

    public statementMap()
    {
        notify = new HashMap<>();
        notifyAll = new HashMap<>();
        notify_entry = new HashMap<>();

        threadNodes = new HashMap<>();
        waiting = new HashMap<>();
        monitor = new HashMap<>();
        labelMap = new HashMap<>();
    }
    public void addStatement(StatementUtil s)
    {
        if(!threadNodes.containsKey(s.thread)){
            threadNodes.put(s.thread, new BitSet(StatementUtil.stmtCount));
        } threadNodes.get(s.thread).set(s.stmtNum);;

        if(s.name == "waiting")
        {
            if(!waiting.containsKey(s.obj)){
                waiting.put(s.obj, new BitSet(StatementUtil.stmtCount));
            } waiting.get(s.obj).set(s.stmtNum);
        } else if(s.name == "label"){
            if(!labelMap.containsKey(s.obj)){
                labelMap.put(s.obj, new ArrayList<>());
            }
            StatementUtil nextStmt = s.getNext().get(0);
            //if(nextStmt.name == "wait") nextStmt = nextStmt.getNext().get(0);
            labelMap.get(s.obj).add(nextStmt);
        }

        for(String iden: s.monitors){
            if(!monitor.containsKey(iden)){
                monitor.put(iden, new BitSet(StatementUtil.stmtCount));
            } monitor.get(iden).set(s.stmtNum);
        }
        if((s.name == "waiting") || (s.name == "notified_entry")){
              if(monitor.containsKey(s.obj)){
                  monitor.get(s.obj).set(s.stmtNum, false);
              }
        }
    }
    public BitSet N(String tName){
      if(!threadNodes.containsKey(tName))
          threadNodes.put(tName, new BitSet(StatementUtil.stmtCount));
      return threadNodes.get(tName);
    }
    public BitSet Monitor(String obj){
      if(!monitor.containsKey(obj))
          monitor.put(obj, new BitSet(StatementUtil.stmtCount));
      return monitor.get(obj);
    }
    public BitSet waitingNodes(String obj){
      if(!waiting.containsKey(obj))
          waiting.put(obj, new BitSet(StatementUtil.stmtCount));
      return waiting.get(obj);
    }
    public ArrayList<StatementUtil> labelNodes(String iden){
      if(!labelMap.containsKey(iden))
          labelMap.put(iden, new ArrayList<StatementUtil>());
      return labelMap.get(iden);
    }
}
