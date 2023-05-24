package symtable;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaseSymbol implements Symbol{
    String name=null;
    Type type=null;

    LocationList useList=new LocationList();

    public BaseSymbol(String name,Type type){
        this.name=name;
        this.type=type;
    }

    public void addLoc(int lineNo,int columnNo){
        useList.add(lineNo,columnNo);
    }

    public LocationList getUseList() {
        return useList;
    }

    @Override
    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setUseList(LocationList useList) {
        this.useList = useList;
    }
}
