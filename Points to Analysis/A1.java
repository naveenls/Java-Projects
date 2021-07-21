import syntaxtree.*;
import visitor.*;
import pointsToUtil.*;

import java.util.*;
import java.awt.*;

public class A1 {
    //Returns the possible function calls based on given Objects
    static ArrayList<Pair<FunctionUtil, BitSet> > getAdjFunc(BitSet objs, String fName)
    {
        Hashtable<FunctionUtil, BitSet> hMap = new Hashtable<>();

        int start = objs.nextSetBit(0);
        while(start != -1)
        {
            ClassUtil c = staticObs.sMap.getObjClass(start);
            FunctionUtil f = c.getParFunc(fName);
            if(f != null)
            {
                if(!hMap.containsKey(f)) hMap.put(f, new BitSet(staticObs.sMap.numObjects));
                hMap.get(f).set(start);
            }
            start = objs.nextSetBit(start+1);
        }
        ArrayList<Pair<FunctionUtil, BitSet> > result = new ArrayList<>();
        hMap.forEach(
            (k, v) -> result.add(new Pair<FunctionUtil, BitSet>(k,v)));

        return result;
    }

    public static void main(String [] args)
    {
        try{
          Node root = new QTACoJavaParser(System.in).Goal();

          initializeUtils<String,StatementUtil> v = new initializeUtils<>();
          root.accept(v, null);

          staticObs.classList.add(new ClassUtil("int[]", "None")); //Creating new class of Array type
          staticObs.setMap();

          for(ClassUtil obj:staticObs.classList)
          {
              if(!obj.getParentName().equals("None")) //Setting parent and child class
              {
                  ClassUtil p = staticObs.getClass(obj.getParentName());
                  obj.setParent(p);
                  p.addChild(obj);
              }
              else {
                  obj.setParent(null);
              }
              obj.initUtils(); //Sets the field types and initializes each variable's rhoMap
          }

          //Analyzation Part
          Queue<FunctionUtil> toProcess = new LinkedList<>(); //Worklist
          toProcess.add(staticObs.mainFunc); //Add main to Worklist
          staticObs.mainFunc.inQueue = true;

          while(!toProcess.isEmpty())
          {
              FunctionUtil f = toProcess.remove();
              f.inQueue = false;
              f.copyInArgs();  //Takes the meet of in Argmutents with actual arguments

              int change = 0;
              for(StatementUtil stmt: f.getStatements(0)) //iden = new iden()
              {
                  int objNum = stmt.objValue;
                  change = Math.max(change, f.meet(stmt.iden1, objNum)); //Adding rhs object to rhoMap of identifier
              }
              for(StatementUtil stmt: f.getStatements(1)) //iden = new int[]()
              {
                  int objNum = stmt.objValue;
                  change = Math.max(change, f.meet(stmt.iden1, objNum)); //Adding rhs object to rhoMap of identifier
              }
              for(StatementUtil stmt: f.getStatements(2)){ //iden = this
                  change = Math.max(change, f.meet(stmt.iden1, f.getThis())); //Adding objects pointed by "this" pointer to rhoMap of identifier
              }
              for(StatementUtil stmt: f.getStatements(3)) //iden = iden
              {
                  BitSet objs = f.getObjs(stmt.iden2);
                  change = Math.max(change, f.meet(stmt.iden1, objs)); //Copying rhoMap of rhs identifier to lhs identifier
              }
              for(StatementUtil stmt: f.getStatements(4)) //iden = iden.iden
              {
                  BitSet objs = f.getFieldObjs(stmt.iden2, stmt.iden3); //Getting all possible objects pointed by field identifier3 of Objects pointed identifier2
                  change = Math.max(change, f.meet(stmt.iden1, objs)); //Copying the objects into rhoMap identifier2
              }
              for(StatementUtil stmt: f.getStatements(5)) //iden = iden.iden(ArgList)
              {
                  BitSet currObj = f.getObjs(stmt.iden2);
                  ArrayList<Pair<FunctionUtil, BitSet> > adjFuncs = getAdjFunc(currObj, stmt.iden3); //Getting possible function calls from objects pointed by identifier2

                  ArrayList<BitSet> arguments = new ArrayList<>(); //Getting objects for each argument
                  arguments.add(null);
                  for(String iden:stmt.ArgList){
                      arguments.add(f.getObjs(iden));
                  }

                  BitSet retVal = new BitSet(staticObs.sMap.numObjects);
                  for(Pair<FunctionUtil, BitSet> pr: adjFuncs)
                  {
                      FunctionUtil adj = pr.getKey();
                      retVal.or(adj.getReturnVal()); //Meet of all possible return Values
                      adj.addInFunc(f); //Adding function f into function adj's in list

                      arguments.set(0, pr.getValue()); //Setting this argument with possible objects
                      if(adj.meet(arguments) && !adj.inQueue){ //Meet of arguments with in argument of adj function
                          toProcess.add(adj); //Formals has changed
                          adj.inQueue = true;
                      }
                  }
                  change = Math.max(change, f.meet(stmt.iden1, retVal)); //Copy the return objects into rhoMap of identifier1
              }
              for(StatementUtil stmt: f.getStatements(6)) //iden = new iden().iden(ArgList)
              {
                  ClassUtil cObj = staticObs.getClass(stmt.iden2);
                  FunctionUtil parFunc = cObj.getParFunc(stmt.iden3); //Getting identifier3 function of identifier2's class

                  ArrayList<BitSet> arguments = new ArrayList<>();
                  BitSet arg0 = new BitSet(staticObs.sMap.numObjects);
                  arg0.set(stmt.objValue);

                  arguments.add(arg0);
                  for(String iden:stmt.ArgList){
                      arguments.add(f.getObjs(iden));
                  }

                  BitSet retVal = parFunc.getReturnVal();
                  parFunc.addInFunc(f);
                  if(parFunc.meet(arguments) && !parFunc.inQueue){
                      toProcess.add(parFunc);
                      parFunc.inQueue = true;
                  }
                  change = Math.max(change, f.meet(stmt.iden1, retVal));
              }
              for(StatementUtil stmt: f.getStatements(7))
              {
                  BitSet currObj = f.getThis();
                  ArrayList<Pair<FunctionUtil, BitSet> > adjFuncs = getAdjFunc(currObj, stmt.iden3); //Getting possible function calls from objects pointed by "this" pointer

                  ArrayList<BitSet> arguments = new ArrayList<>();
                  arguments.add(null);
                  for(String iden:stmt.ArgList){
                      arguments.add(f.getObjs(iden));
                  }

                  BitSet retVal = new BitSet(staticObs.sMap.numObjects);
                  for(Pair<FunctionUtil, BitSet> pr: adjFuncs)
                  {
                      FunctionUtil adj = pr.getKey();
                      retVal.or(adj.getReturnVal());
                      adj.addInFunc(f);

                      arguments.set(0, pr.getValue());
                      if(adj.meet(arguments) && !adj.inQueue){
                          toProcess.add(adj);
                          adj.inQueue = true;
                      }
                  }
                  change = Math.max(change, f.meet(stmt.iden1, retVal));
              }
              for(StatementUtil stmt: f.getStatements(8))
              {
                  BitSet lhsObjs = f.getObjs(stmt.iden1);
                  BitSet rhsObjs = f.getObjs(stmt.iden3);
                  boolean flag = false;

                  int start = lhsObjs.nextSetBit(0);
                  while(start != -1)
                  {
                      flag |= staticObs.sMap.addField(start, stmt.iden2, rhsObjs);
                      start = lhsObjs.nextSetBit(start+1);
                  }
                  if(flag) change = 2;
              }

              BitSet newRetVal = new BitSet(staticObs.sMap.numObjects);
              for(StatementUtil stmt: f.getStatements(9)){ //Getting the new possible return objects
                  BitSet objs = f.getObjs(stmt.iden1);
                  newRetVal.or(objs);
              }
              if(f.meetRet(newRetVal)){ //If return objects has changed then add possible in functions to Worklist
                f.inFuncs.forEach((k, fun) -> {
                      if(!fun.inQueue){
                          fun.inQueue = true;
                          toProcess.add(fun);
                      }
                });
              }

              if(change == 1){ //rhoMap of current function is changed
                  f.inQueue = true;
                  toProcess.add(f);
              } else if(change == 2){ //Heap has changed, add all functions to Worklist
                  for(ClassUtil obj:staticObs.classList){
                      for(FunctionUtil fun:obj.getFuncs()){
                          if(!fun.inQueue){
                              fun.inQueue = true;
                              toProcess.add(fun);
                          }
                      }
                  }
              }
          }

          for(ClassUtil c: staticObs.classList){ //Answer the queries
              for(FunctionUtil f: c.getFuncs()){
                  f.answerQueries();
              }
          }
        }
        catch (ParseException e) {
            System.out.println(e.toString());
        }
    }
}
