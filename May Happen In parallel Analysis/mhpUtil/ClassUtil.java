package mhpUtil;
import java.util.*;
import java.awt.*;

public class ClassUtil {
  String className;
  boolean isThread;

  FunctionUtil runM;
  ArrayList<Pair<String,String> > fields;

  public ClassUtil(String cName, boolean isThread){
    this.fields = new ArrayList<>();
    className = cName;
    this.isThread = isThread;
  }

  public void addField(String name,String type)
  {
    if(type == "int" || type == "boolean")
      type = "void";

    this.fields.add(new Pair<String,String>(name, type));
  }

  public void setRun(FunctionUtil f){
    this.runM = f;
  }
  public FunctionUtil getRun() {
  	return runM;
  }
  public String getClassName() {
  	return className;
  }
  public String getFieldType(String name)
  {
      for(Pair<String,String> pr:fields)
      {
          String fName = pr.getKey();
          if(fName.equals(name)) return pr.getValue();
      }
      return "void";
  }
}
