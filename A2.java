import syntaxtree.*;
import visitor.*;
import mhpUtil.*;

import java.util.*;
import java.awt.*;

public class A2 {
    private static HashMap<String, StatementUtil> threadMap = new HashMap<>();
    private static StatementUtil bNode = new StatementUtil(null, "begin", null);
    private static StatementUtil eNode = new StatementUtil(null, "end", null);

    static StatementUtil cloneGraph(StatementUtil root, HashMap<StatementUtil, StatementUtil> processed, String tName)
    {
        if(root == null) return null;
        if(processed.containsKey(root)) return processed.get(root);

        StatementUtil sNew = root.getCopy(tName);
        processed.put(root, sNew);

        for(StatementUtil ch: root.getNext())
        {
            StatementUtil newCh = cloneGraph(ch, processed, tName);
            sNew.addNext(newCh);
        }
        return sNew;
    }
    static StatementUtil buildPEG(FunctionUtil fn, String thread)
    {
        if(threadMap.containsKey(thread)) return threadMap.get(thread);

        StatementUtil beginNode = bNode.getCopy(thread);

        HashMap<StatementUtil, StatementUtil> processed = new HashMap<>();
        StatementUtil startCpy = cloneGraph(fn.start, processed, thread);
        StatementUtil endCpy = processed.get(fn.end);
        StatementUtil endNode = eNode.getCopy(thread);

        for(StatementUtil s: processed.keySet())
        {
            StatementUtil sCpy = processed.get(s);
            if(sCpy.name == "start")
            {
                String iden = sCpy.obj;
                String sType = fn.getType(iden);
                FunctionUtil chFun = staticObs.getClass(sType).getRun();

                StatementUtil newThread = buildPEG(chFun, iden);
                sCpy.addNext(newThread);
            }
        }

        beginNode.addNext(startCpy);
        endCpy.addNext(endNode);
        threadMap.put(thread, beginNode);

        return beginNode;
    }

    public static void main(String [] args)
    {
        try{
          Node root = new QParJavaParser(System.in).Goal();

          getStatements<Object,Object> v = new getStatements<>();
          root.accept(v, null);
          staticObs.setMap();

          //Building PEG
          StatementUtil headNode = buildPEG(staticObs.mainFunc, "main");

          //Initialization
          Queue<StatementUtil> toProcess = new LinkedList<>(); //Worklist

          for(StatementUtil s: StatementUtil.stmtList) staticObs.sMap.addStatement(s);
          for(StatementUtil s: StatementUtil.stmtList){
            s.initSets();
            if(s.name == "start" && s.thread == "main"){
                toProcess.add(s);
                s.inQueue = true;
            }
          }
          //Analyzation Part

          while(!toProcess.isEmpty())
          {
              StatementUtil stmt = toProcess.remove();
              stmt.inQueue = false;
              stmt.computeMhp();

              //System.out.println(stmt.obj + " " + stmt.name + " " + stmt.thread);
              if(stmt.name == "notify" || stmt.name == "notifyAll")
              {
                  BitSet notifySuccPar = (BitSet)stmt.mhp.clone();
                  notifySuccPar.and(staticObs.sMap.waitingNodes(stmt.obj));

                  BitSet notifySucc = new BitSet(StatementUtil.stmtCount);
                  int start = notifySuccPar.nextSetBit(0);
                  while(start != -1)
                  {
                      StatementUtil nEntry = StatementUtil.stmtList.get(start).getNotifyEntry();
                      notifySucc.set(nEntry.stmtNum);
                      nEntry.addNotifyPred(stmt);

                      start = notifySuccPar.nextSetBit(start+1);
                  }

                  if(!stmt.notifySucc.equals(notifySucc))
                  {
                        stmt.gen = (BitSet)notifySucc.clone();
                        stmt.notifySucc = notifySucc;

                        start = notifySucc.nextSetBit(0);
                        while(start != -1)
                        {
                            StatementUtil succ = StatementUtil.stmtList.get(start);
                            if(!succ.inQueue){
                                toProcess.add(succ);
                                succ.inQueue = true;
                            }
                            start = notifySucc.nextSetBit(start+1);
                        }
                  }
              }

              int start = stmt.mhp.nextSetBit(0);
              while(start != -1)
              {
                  StatementUtil adj = StatementUtil.stmtList.get(start);
                  if(adj.mhp.get(stmt.stmtNum) == false){
                      adj.mhp.set(stmt.stmtNum);
                      if(!adj.inQueue){
                          toProcess.add(adj);
                          adj.inQueue = true;
                      }
                  }
                  start = stmt.mhp.nextSetBit(start+1);
              }

              if(stmt.computeOut()){
                  for(StatementUtil ch: stmt.getNext())
                      if(!ch.inQueue){
                          toProcess.add(ch);
                          ch.inQueue = true;
                      }
              }
          }

          for(Pair<String, String> pr: staticObs.queries)
          {
              ArrayList<StatementUtil> lst1 = staticObs.sMap.labelNodes(pr.getKey());
              ArrayList<StatementUtil> lst2 = staticObs.sMap.labelNodes(pr.getValue());

              boolean isMHP = false;
              for(StatementUtil s1: lst1){
                  for(StatementUtil s2: lst2){
                      isMHP |= (s1.mhp.get(s2.stmtNum));
                  }
              }

              if(isMHP)
                System.out.print("Yes\n");
              else
                System.out.print("No\n");
          }
        }
        catch (ParseException e) {
            System.out.println(e.toString());
        }
    }
}
