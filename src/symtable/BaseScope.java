package symtable;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope{

    String name;
    Map<String,Symbol> symbolMap=new LinkedHashMap<>();
    Scope enclosingScope;
    @Override
    public void setName(String name) {
        this.name=name;
    }

    @Override
    public Map<String, Symbol> getSymbols() {
        return symbolMap;
    }

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    public void setEnclosingScope(Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    @Override
    public void define(Symbol symbol) {
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
    public Symbol resolve(String name) {
        Symbol symbol=symbolMap.get(name);
        if(symbol!=null) {
            System.out.println("*" + name);
            return symbol;
        }
        if(enclosingScope!=null){
            return enclosingScope.resolve(name);
        }
        System.out.println("this scope and its fathers can not find "+name);
        return null;
    }

    @Override
    public Symbol resolveMyself(String name) {
        Symbol symbol=symbolMap.get(name);
        if(symbol!=null) {
            System.out.println("*" + name);
            return symbol;
        }
        System.out.println("this scope  can not find "+name);
        return null;
    }
}
