package mhpUtil;
import java.util.*;
import java.awt.*;

public class VariableUtil{
  String name;
  FunctionUtil parentFunc;
  String type;

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
}
