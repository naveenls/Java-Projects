package pointsToUtil;
import java.util.*;
import java.awt.*;

class heapObject
{
    int objNum;
    public String objType;
    public Hashtable<String,BitSet> fieldsMap;

    public heapObject(int objNum, String type){
        this.objNum = objNum;
        fieldsMap = new Hashtable<>();
        objType = type;
    }

}

public class SigmaMap
{
        ArrayList<heapObject> objects;
        public int numObjects;

        public SigmaMap(){
            objects = new ArrayList<>();
            numObjects = 0;
        }
        public int addObject(String cName)
        {
          heapObject obj = new heapObject(numObjects, cName);
          objects.add(obj);
          return numObjects++;
        }

        public boolean addField(int objNum, String field, BitSet objs)
        {
            heapObject hp = objects.get(objNum);
            if(!hp.fieldsMap.containsKey(field)){
                hp.fieldsMap.put(field, new BitSet(staticObs.sMap.numObjects));
            }

            BitSet b = hp.fieldsMap.get(field);
            BitSet bTemp = (BitSet)b.clone();
            b.or(objs);

            return !b.equals(bTemp);
        }

        public BitSet getField(int objNum, String field)
        {
            heapObject hp = objects.get(objNum);
            if(!hp.fieldsMap.containsKey(field)){
                hp.fieldsMap.put(field, new BitSet(staticObs.sMap.numObjects));
            }
            return hp.fieldsMap.get(field);
        }

        public ClassUtil getObjClass(int objNum)
        {
            heapObject hp = objects.get(objNum);
            return staticObs.getClass(hp.objType);
        }
}
