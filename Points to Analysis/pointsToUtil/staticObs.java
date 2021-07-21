package pointsToUtil;
import java.util.*;
import java.awt.*;

public class staticObs{
  public static ClassUtil currClass = null;
  public static FunctionUtil currFunc = null;
  public static FunctionUtil mainFunc;
   
  public static ArrayList<ClassUtil> classList = new ArrayList<>();
  public static Hashtable<String,ClassUtil> classMap;
  public static SigmaMap sMap = new SigmaMap();

  public static void setMap(){
      classMap = new Hashtable<>();
      for(ClassUtil c:classList){
          String name = c.getClassName();
          classMap.put(name, c);
      }
  }

  public static ClassUtil getClass(String name){
    return classMap.get(name);
  }
}
