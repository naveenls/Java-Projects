package pointsToUtil;
import java.util.*;
import java.awt.*;

public class FunctionUtil{

  String funcName;
  ClassUtil parentClass;
  public SymbolTable sTable;

  ArrayList<Pair<String,String> > queries;
  public Hashtable<String, FunctionUtil> inFuncs;

  public Object[] statements;
  public boolean inQueue;

  public FunctionUtil(String fName, ClassUtil pClass){
      funcName = fName;
      parentClass = pClass;
      inQueue = false;

      sTable = new SymbolTable();
      statements = new Object[10];
      for(int i=0; i<10; i++)
        statements[i] = new ArrayList<StatementUtil>();
      queries = new ArrayList<>();
      inFuncs = new Hashtable<>();
  }

  public ClassUtil getParentClass() {
  	return parentClass;
  }
  public String getfuncName(){
      return funcName;
  }
  public BitSet getReturnVal(){
    return sTable.returnVal;
  }
  public void addLocal(VariableUtil v){
    sTable.addLocal(v);
  }
  public void addArg(VariableUtil v){
    sTable.addArg(v);
  }
  public void addQuery(String var1, String var2){
      queries.add(new Pair<String,String>(var1, var2));
  }
  public void addInFunc(FunctionUtil par){
      ClassUtil parClass = par.getParentClass();
      String key = parClass.getClassName() + "#" + par.getfuncName();

      inFuncs.put(key, par);
  }
  public void initVariables()
  {
      sTable.returnVal = new BitSet(staticObs.sMap.numObjects);
      sTable.thisPtr = new BitSet(staticObs.sMap.numObjects);

      sTable.locals.forEach(
          (k, v) -> v.setObjType());
      sTable.args.forEach(
          (k, v) -> v.setObjType());

      for(int i=0;i<sTable.inArgs.size();i++){
          Pair<String, BitSet> pr = sTable.inArgs.get(i);
          pr.setValue(new BitSet(staticObs.sMap.numObjects));
      }
  }
  public String getType(String name)
  {
      if(sTable.locals.containsKey(name)){
          VariableUtil v = sTable.locals.get(name);
          return v.getType();
      }

      if(sTable.args.containsKey(name)){
          VariableUtil v = sTable.args.get(name);
          return v.getType();
      }

      return parentClass.getFieldType(name);
  }
  public void addStatement(StatementUtil s)
  {
    int type = s.type;
    ArrayList<StatementUtil> stmt = (ArrayList<StatementUtil>)statements[type];
    stmt.add(s);
  }
  public ArrayList<StatementUtil> getStatements(int type){
      return (ArrayList<StatementUtil>)statements[type];
  }

  public void copyInArgs(){
      sTable.copyInArgs();
  }

  public BitSet filter(BitSet b, ClassUtil argClass){
    BitSet newSet = new BitSet(staticObs.sMap.numObjects);

    int start = b.nextSetBit(0);
    while(start != -1)
    {
        ClassUtil cType = staticObs.sMap.getObjClass(start);
        if(argClass.isValidSubclass(cType))  newSet.set(start);
        start = b.nextSetBit(start+1);
    }
    return newSet;
  }

  public boolean meet(ArrayList<BitSet> inArgs)
  {
      boolean flag = false;
      BitSet temp = (BitSet)sTable.thisPtr.clone();

      BitSet newThis = inArgs.get(0);
      sTable.thisPtr.or(newThis);
      flag |= !(sTable.thisPtr.equals(temp));

      for(int i=1;i<inArgs.size();i++)
      {
          Pair<String, BitSet> pr = sTable.inArgs.get(i-1);
          temp = (BitSet)pr.getValue().clone();

          BitSet newArg = filter(inArgs.get(i), sTable.getArgClass(pr.getKey()));
          //BitSet newArg = inArgs.get(i);
          pr.getValue().or(newArg);
          flag |= !(pr.getValue().equals(temp));
      }

      return flag;
  }

  public boolean meetRet(BitSet newRetVal)
  {
      BitSet temp = (BitSet)sTable.returnVal.clone();
      sTable.returnVal.or(newRetVal);

      return !sTable.returnVal.equals(temp);
  }
  public int meet(String iden,int objNum)
  {
    BitSet b = new BitSet(staticObs.sMap.numObjects);
    b.set(objNum);

    return meet(iden, b);
  }

  public int meet(String iden,BitSet objs)
  {
    if(sTable.locals.containsKey(iden)){
        VariableUtil v = sTable.locals.get(iden);
        BitSet bTemp = (BitSet)v.pointsTo.clone();
        v.pointsTo.or(objs);

        return bTemp.equals(v.pointsTo) ? 0 : 1;
    }
    else if(sTable.args.containsKey(iden)){
        VariableUtil v = sTable.args.get(iden);
        BitSet bTemp = (BitSet)v.pointsTo.clone();
        v.pointsTo.or(objs);

        return bTemp.equals(v.pointsTo) ? 0 : 1;
    }
    else{
        boolean flag = false;
        int start = sTable.thisPtr.nextSetBit(0);

        while(start != -1)
        {
            flag |= staticObs.sMap.addField(start,iden,objs);
            start = sTable.thisPtr.nextSetBit(start+1);
        }
        return flag ? 2 : 0;
    }
  }

  public BitSet getObjs(BitSet objs, String iden)
  {
      BitSet result = new BitSet(staticObs.sMap.numObjects);
      int start = objs.nextSetBit(0);

      while(start != -1)
      {
          result.or(staticObs.sMap.getField(start,iden));
          start = objs.nextSetBit(start+1);
      }
      return result;
  }

  public BitSet getObjs(String iden)
  {
      if(sTable.locals.containsKey(iden)){
          VariableUtil v = sTable.locals.get(iden);
          return v.pointsTo;
      }
      else if(sTable.args.containsKey(iden)){
          VariableUtil v = sTable.args.get(iden);
          return v.pointsTo;
      }
      else{
          return getObjs(sTable.thisPtr, iden);
      }
    }

    public BitSet getThis(){
      return sTable.thisPtr;
    }

    public BitSet getFieldObjs(String iden, String field)
    {
        if(sTable.locals.containsKey(iden)){
            VariableUtil v = sTable.locals.get(iden);
            return getObjs(v.pointsTo, field);
        }
        else if(sTable.args.containsKey(iden)){
            VariableUtil v = sTable.args.get(iden);
            return getObjs(v.pointsTo, field);
        }
        else{
            BitSet objs = getObjs(sTable.thisPtr, iden);
            return getObjs(objs, field);
        }
    }

    public void answerQueries()
    {
        for(Pair<String, String> pr: queries){
            String iden1 = pr.getKey();
            String iden2 = pr.getValue();

            BitSet b1 = getObjs(iden1);
            BitSet b2 = getObjs(iden2);

            if(b1.intersects(b2))
              System.out.println("Yes");
            else
              System.out.println("No");
        }
    }
}
