package pointsToUtil;
import java.util.*;
import java.awt.*;

public class ClassUtil {
  String className;

  ClassUtil parent;
  String parentName;

  ArrayList<ClassUtil> child;
  ArrayList<FunctionUtil> funcs;
  ArrayList<Pair<String,ClassUtil> > fields;

  public ClassUtil(String cName, String pName){
    this.child = new ArrayList<>();
    this.funcs = new ArrayList<>();
    this.fields = new ArrayList<>();
    parentName = pName;
    className = cName;
  }

  public void setParent(ClassUtil parent) {
  	this.parent = parent;
  }
  public void addChild(ClassUtil chClass) {
     this.child.add(chClass);
  }
  public void addFuncs(FunctionUtil func){
     this.funcs.add(func);
  }
  public void addField(String name,String type)
  {
    if(type == "int" || type == "boolean")
      type = "void";

    ClassUtil c = new ClassUtil(type, "None");
    this.fields.add(new Pair<String,ClassUtil>(name, c));
  }

  public String getParentName() {
  	return parentName;
  }
  public String getClassName() {
  	return className;
  }
  public ClassUtil getParent() {
  	return parent;
  }
  public ArrayList<ClassUtil> getChild() {
  	return child;
  }
  public ArrayList<FunctionUtil> getFuncs() {
    return funcs;
  }
  public String getFieldType(String name)
  {
      for(Pair<String,ClassUtil> pr:fields)
      {
          String fName = pr.getKey();
          if(fName.equals(name)){
              ClassUtil c = pr.getValue();
              return c.getClassName();
          }
      }
      if(parent == null)  return "void";
      return parent.getFieldType(name);
  }

  public FunctionUtil getParFunc(String fName)
  {
      ClassUtil c = this;
      while(c != null)
      {
         for(FunctionUtil f: c.getFuncs()){
             if(f.getfuncName().equals(fName))
                return f;
         }
         c = c.getParent();
      }
      return null;
  }

  public boolean isValidSubclass(ClassUtil c){
    Queue<ClassUtil> childLst = new LinkedList<>();
    childLst.add(this);

    while(!childLst.isEmpty()){
        ClassUtil ch = childLst.remove();
        if(ch == c) return true;

        for(ClassUtil ch1: ch.getChild()){
            childLst.add(ch1);
        }
    }
    return false;
  }

  public void initUtils()
  {
      for(int i=0;i<fields.size();i++)
      {
          Pair<String,ClassUtil> pr = fields.get(i);
          String cName = pr.getValue().getClassName();
          if(cName == "void") continue;

          ClassUtil classObj = staticObs.getClass(cName);
          pr.setValue(classObj);
      }
      for(FunctionUtil f:funcs){
          f.initVariables();
      }
  }
}
