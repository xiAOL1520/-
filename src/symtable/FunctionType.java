package symtable;

import java.util.ArrayList;

public class FunctionType extends Type{
    String typename="function";
    Type retype;
    ArrayList<Type> paramstype=new ArrayList<>();

    public void setRetype(Type type){
        this.retype=type;
    }

    public void setParamstype(ArrayList<Type> paramstype){
        this.paramstype=paramstype;
    }

    public void addParamType(Type paramType){
        paramstype.add(paramType);
    }

    public String getTypename() {
        return typename;
    }

    public Type getRetype() {
        return retype;
    }

    public ArrayList<Type> getParamstype() {
        return paramstype;
    }
}
