package mhpUtil;
import java.util.*;
import java.awt.*;

public class staticObs{
  public static ClassUtil currClass = null;
  public static FunctionUtil currFunc = null;
  public static FunctionUtil mainFunc;

  public static ArrayList<ClassUtil> classList = new ArrayList<>();
  public static Hashtable<String,ClassUtil> classMap;
  public static ArrayList<Pair<String,String> > queries = new ArrayList<>();
  public static LinkedList<String> monitors = new LinkedList<>();
  public static statementMap sMap = new statementMap();

  public static void setMap(){
      classMap = new Hashtable<>();
      for(ClassUtil c:classList){
          String name = c.getClassName();
          classMap.put(name, c);
      }
  }
  public static void addQuery(String var1, String var2){
      queries.add(new Pair<String,String>(var1, var2));
  }
  public static ClassUtil getClass(String name){
    return classMap.get(name);
  }
}
