package llsymtable;


import java.util.Map;

public interface LLScope {
    public boolean isFunc();
    public void setName(String name);

    public Map<String, LLSymbol> getSymbols();

    public LLScope getEnclosingScope();

    public void define(LLSymbol symbol);

    public String getName();

    public LLSymbol resolve(String name);
}
