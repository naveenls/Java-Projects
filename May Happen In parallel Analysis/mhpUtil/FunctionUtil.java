package mhpUtil;
import java.util.*;
import java.awt.*;

public class FunctionUtil{
  ClassUtil parentClass;
  HashMap<String, VariableUtil> locals;

  Stack<ArrayList<Pair<StatementUtil, StatementUtil> > > statements;
  public StatementUtil start, end;

  public boolean inQueue;

  public FunctionUtil(ClassUtil pClass){
      parentClass = pClass;
      inQueue = false;

      locals = new HashMap<>();
      statements = new Stack<>();
      statements.push(new ArrayList<>());
  }

  public ClassUtil getParentClass() {
  	return parentClass;
  }
  public void addLocal(VariableUtil v){
    locals.put(v.getName(), v);
  }
  public String getType(String name)
  {
      if(locals.containsKey(name)){
          VariableUtil v = locals.get(name);
          return v.getType();
      }
      return parentClass.getFieldType(name);
  }
  public void beginScope()
  {
      ArrayList<Pair<StatementUtil, StatementUtil> > lst = new ArrayList<>();
      statements.push(lst);
  }
  public void endScope()
  {
      ArrayList<Pair<StatementUtil, StatementUtil> > lst = statements.pop();
      if(!lst.isEmpty())
      {
          StatementUtil first = lst.get(0).getKey();
          StatementUtil last = lst.get(0).getValue();

          for(int i=1;i<lst.size();i++)
          {
              last.addNext(lst.get(i).getKey());
              last = lst.get(i).getValue();
          }

          lst = statements.peek();
          lst.add(new Pair<>(first, last));
      } else {
          StatementUtil sNew = new StatementUtil(null, "dummyStmt", null);
          lst.add(new Pair<>(sNew, sNew));
      }
  }
  public void addStatement(StatementUtil s){
    statements.peek().add(new Pair<>(s, s));
  }
  public void addStatement(StatementUtil s1, StatementUtil s2){
    statements.peek().add(new Pair<>(s1, s2));
  }
  public Pair<StatementUtil, StatementUtil> popBack(){
      ArrayList<Pair<StatementUtil, StatementUtil> > lst = statements.peek();
      return lst.remove(lst.size()-1);
  }
  public void initSE(){
      ArrayList<Pair<StatementUtil, StatementUtil> > lst = statements.pop();
      statements = null;

      if(!lst.isEmpty()){
        start = lst.get(0).getKey();
        end = lst.get(0).getValue();
      }
  }
}
