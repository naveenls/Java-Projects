package pointsToUtil;
import java.util.*;
import java.awt.*;

public class StatementUtil
{
    public int type;
    public String iden1;
    public String iden2;
    public String iden3;

    public ArrayList<String> ArgList;
    public int objValue;

    public StatementUtil(){
        type = -1;
        ArgList = new ArrayList<>();
    }

    public void addArg(String iden){
          ArgList.add(iden);
    }
}
