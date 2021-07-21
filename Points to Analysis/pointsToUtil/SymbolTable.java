package pointsToUtil;
import java.util.*;
import java.awt.*;

public class SymbolTable
{
    public Hashtable<String, VariableUtil> locals;
    public Hashtable<String, VariableUtil> args;
    public ArrayList<Pair<String, BitSet> > inArgs;

    public BitSet thisPtr;
    public BitSet returnVal;

    public SymbolTable(){
      locals = new Hashtable<>();
      args = new Hashtable<>();
      inArgs = new ArrayList<>();
    }

    public void addLocal(VariableUtil v){
        locals.put(v.getName(),v);
    }
    public void addArg(VariableUtil v){
        args.put(v.getName(),v);
        inArgs.add(new Pair<String,BitSet>(v.getName(), null));
    }
    public ClassUtil getArgClass(String argName){
      VariableUtil v = args.get(argName);
      if(v.getType() == "void") return null;
      return staticObs.getClass(v.getType());
    }
    public void copyInArgs(){
        for(Pair<String, BitSet> arg: inArgs)
        {
            String argName = arg.getKey();
            BitSet inObs = arg.getValue();

            VariableUtil v = args.get(argName);
            v.pointsTo.or(inObs);
        }
    }
}
