package llsymtable;


import symtable.Symbol;

import java.util.LinkedHashMap;
import java.util.Map;

public class LLBaseScope implements LLScope {

    Boolean isfunc=false;
    String name="";

    Map<String, LLSymbol> symbolMap=new LinkedHashMap<>();

    LLScope enclosingScope;

    public void setIsfunc(Boolean isfunc) {
        this.isfunc = isfunc;
    }

    @Override
    public boolean isFunc(){
        return isfunc;
    }

    @Override
    public void setName(String name) {
        this.name=name;
    }

    public void setEnclosingScope(LLScope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    @Override
    public Map<String, LLSymbol> getSymbols() {
        return symbolMap;
    }

    @Override
    public LLScope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void define(LLSymbol symbol) {
        if(!symbolMap.containsKey(symbol.getName())){
            symbolMap.put(symbol.getName(),symbol);
            System.out.println("+"+symbol.getName());
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LLSymbol resolve(String name) {
        LLSymbol symbol=symbolMap.get(name);
        if(symbol!=null) {
            System.out.println("*" + name);
            return symbol;
        }
        if(enclosingScope!=null){
            return enclosingScope.resolve(name);
        }
        System.out.println("this LLscope and its fathers can not find "+name);
        return null;
    }
}
