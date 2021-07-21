package pointsToUtil;
import java.util.*;
import java.awt.*;

public class VariableUtil{
  String name;
  FunctionUtil parentFunc;
  String type;
  ClassUtil objType;
  public BitSet pointsTo;

  public VariableUtil(String varName, String type, FunctionUtil parFunc){
    name = varName;
    this.parentFunc = parFunc;

    if(type.equals("int") || type.equals("boolean"))
      type = "void";

    this.type = type;
  }
  public String getName() {
  	return name;
  }
  public String getType() {
    return type;
  }
  public void setObjType() {
    pointsTo = new BitSet(staticObs.sMap.numObjects);
    if(this.type == "void")
      objType = null;
    else
  	  objType = staticObs.getClass(this.type);
  }
}
