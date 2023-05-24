package symtable;

import java.util.Map;

public interface Symbol {

    public String getName();

    public Type getType();

    void addLoc(int lineNo, int columnNo);

    public LocationList getUseList();
}
