package symtable;

import java.util.ArrayList;

public class LocationList {
    ArrayList<Location> list=new ArrayList<>();

    public void add(int lineNo,int columnNo){
        list.add(new Location(lineNo,columnNo));
    }

    public boolean containsLoc(int lineNo,int columnNo){
        int length=list.size();
        for(int i=0;i<length;i++){
            Location location=list.get(i);
            if(location.ifSame(lineNo,columnNo)){
               return true;
            }
        }
        return false;
    }
}
