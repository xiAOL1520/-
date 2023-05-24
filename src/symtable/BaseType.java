package symtable;

public class BaseType extends Type{
    String typename;

    public BaseType(String typename){
        this.typename=typename;
    }

    public void setTypename(String typename) {
        this.typename = typename;
    }

    public String getTypename() {
        return typename;
    }
}
